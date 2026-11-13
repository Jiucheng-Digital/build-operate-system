package com.neucity.build.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.neucity.build.exception.SpaceDocumentException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Independent validation of the Space v1 contract; never transform local coordinates. */
@Component
public class SpaceSnapshotValidator {
    private static final double ROUNDING_TOLERANCE = 0.01 + 1e-9;
    private static final double BOUNDARY_TOLERANCE = 1e-9;

    public void validate(JsonNode snapshot) {
        object(snapshot, "snapshot");
        JsonNode version = snapshot.path("version");
        require(version.isIntegralNumber() && version.canConvertToInt() && version.intValue() == 1,
                "仅支持 snapshot.version = 1，不能将 version 用作 revision");
        String spaceId = text(snapshot, "id", 36);
        try {
            require(UUID.fromString(spaceId).toString().equalsIgnoreCase(spaceId), "snapshot.id 必须为 UUID");
        } catch (IllegalArgumentException exception) {
            throw SpaceDocumentException.invalid("snapshot.id 必须为 UUID");
        }
        text(snapshot, "name", 120);
        object(snapshot.path("origin"), "origin");
        number(snapshot.path("origin").path("x"), "origin.x");
        number(snapshot.path("origin").path("y"), "origin.y");
        positive(snapshot.path("scale"), "scale");
        positive(snapshot.path("gridSize"), "gridSize");
        text(snapshot, "unit", 32);
        bool(snapshot, "gridVisible");
        text(snapshot, "gridColor", 120);
        opacity(snapshot.path("gridOpacity"), "gridOpacity", true);
        object(snapshot.path("bounds"), "bounds");
        double halfWidth = positive(snapshot.path("bounds").path("width"), "bounds.width") / 2;
        double halfDepth = positive(snapshot.path("bounds").path("depth"), "bounds.depth") / 2;
        JsonNode publishedAt = snapshot.path("publishedAt");
        require(publishedAt.isNull() || publishedAt.isTextual(), "publishedAt 必须为字符串或 null");
        validateMap(snapshot.path("map"));

        Map<String, JsonNode> layers = index(snapshot, "layers", "id");
        Map<String, JsonNode> columns = index(snapshot, "columns", "id");
        Map<String, JsonNode> devices = index(snapshot, "devices", "deviceId");
        Map<String, JsonNode> bindings = index(snapshot, "bindings", "id");
        for (JsonNode layer : layers.values()) {
            text(layer, "name", 120);
            integer(layer.path("order"), "layers[].order");
            bool(layer, "visible");
            bool(layer, "locked");
        }
        for (JsonNode column : columns.values()) {
            validateColumn(column, layers, halfWidth, halfDepth);
        }
        Set<String> pointIds = new HashSet<>();
        for (JsonNode device : devices.values()) {
            require(pointIds.add(text(device, "id", 128)), "devices[].id 不能重复");
            text(device, "name", 120);
            text(device, "type", 128);
            text(device, "status", 128);
            // A displayed device coordinate may already have been rounded to two decimals.
            point(device, "devices[]", halfWidth, halfDepth, ROUNDING_TOLERANCE);
            number(device.path("z"), "devices[].z");
        }
        Set<String> boundDevices = new HashSet<>();
        for (JsonNode binding : bindings.values()) {
            validateBinding(binding, spaceId, layers, columns, devices, boundDevices, halfWidth, halfDepth);
        }
    }

    private void validateColumn(JsonNode column, Map<String, JsonNode> layers, double halfWidth, double halfDepth) {
        String type = text(column, "type", 32);
        require("box".equals(type) || "cube".equals(type) || "lshape".equals(type)
                || "cylinder".equals(type) || "polygon".equals(type), "体块 type 不受支持: " + type);
        text(column, "name", 120);
        String layerId = text(column, "layerId", 128);
        require(layers.containsKey(layerId), "体块引用的图层不存在: " + layerId);
        integer(column.path("order"), "columns[].order");
        point(column, "columns[]", halfWidth, halfDepth, BOUNDARY_TOLERANCE);
        double width = positive(column.path("width"), "columns[].width");
        double depth = positive(column.path("depth"), "columns[].depth");
        nonNegative(column.path("height"), "columns[].height");
        number(column.path("rotation"), "columns[].rotation");
        opacity(column.path("opacity"), "columns[].opacity", false);
        text(column, "fillColor", 120);
        text(column, "strokeColor", 120);
        nonNegative(column.path("strokeWidth"), "columns[].strokeWidth");
        bool(column, "showLabel");
        bool(column, "locked");
        bool(column, "visible");
        JsonNode points = column.path("points");
        require(points.isArray(), "columns[].points 必须为数组");
        require(!"polygon".equals(type) || points.size() >= 3, "polygon 必须包含至少 3 个顶点");
        for (JsonNode vertex : points) {
            object(vertex, "columns[].points[]");
            number(vertex.path("x"), "columns[].points[].x");
            number(vertex.path("y"), "columns[].points[].y");
        }
        if ("lshape".equals(type)) {
            double notchWidth = positive(column.path("notchWidth"), "columns[].notchWidth");
            double notchDepth = positive(column.path("notchDepth"), "columns[].notchDepth");
            require(notchWidth < width && notchDepth < depth, "lshape 缺口尺寸必须小于体块宽深");
        } else {
            if (column.has("notchWidth")) {
                nonNegative(column.get("notchWidth"), "columns[].notchWidth");
            }
            if (column.has("notchDepth")) {
                nonNegative(column.get("notchDepth"), "columns[].notchDepth");
            }
        }
    }

    private void validateBinding(JsonNode binding, String spaceId, Map<String, JsonNode> layers,
                                 Map<String, JsonNode> columns, Map<String, JsonNode> devices,
                                 Set<String> boundDevices, double halfWidth, double halfDepth) {
        String deviceId = text(binding, "deviceId", 128);
        JsonNode device = devices.get(deviceId);
        require(device != null, "绑定的设备不存在: " + deviceId);
        require(boundDevices.add(deviceId), "设备不能重复绑定: " + deviceId);
        String type = text(binding, "targetType", 32);
        String targetId = text(binding, "targetId", 128);
        double localX = number(binding.path("localX"), "bindings[].localX");
        double localY = number(binding.path("localY"), "bindings[].localY");
        double z = number(binding.path("z"), "bindings[].z");
        string(binding.path("label"), "bindings[].label");
        bool(binding, "showLine");
        double x = localX;
        double y = localY;
        if ("column".equals(type)) {
            JsonNode column = columns.get(targetId);
            require(column != null, "绑定的体块不存在: " + targetId);
            double theta = Math.toRadians(column.path("rotation").doubleValue());
            x = column.path("x").doubleValue() + localX * Math.cos(theta) - localY * Math.sin(theta);
            y = column.path("y").doubleValue() + localX * Math.sin(theta) + localY * Math.cos(theta);
        } else if ("layer".equals(type)) {
            require(layers.containsKey(targetId), "绑定的图层不存在: " + targetId);
        } else if ("space".equals(type)) {
            require(spaceId.equals(targetId), "绑定的空间不存在: " + targetId);
        } else {
            throw SpaceDocumentException.invalid("绑定 targetType 仅支持 space、layer、column");
        }
        withinBounds(x, y, halfWidth, halfDepth, BOUNDARY_TOLERANCE, "绑定点");
        require(Math.abs(device.path("x").doubleValue() - x) <= ROUNDING_TOLERANCE
                        && Math.abs(device.path("y").doubleValue() - y) <= ROUNDING_TOLERANCE,
                "设备点位 x/y 与绑定目标及局部偏移不一致: " + deviceId);
        require(Math.abs(device.path("z").doubleValue() - z) <= ROUNDING_TOLERANCE,
                "设备点位 z 与绑定高度不一致: " + deviceId);
    }

    private void validateMap(JsonNode map) {
        require(!map.isMissingNode(), "snapshot 必须包含 map（未上传时为 null）");
        if (map.isNull()) {
            return;
        }
        object(map, "map");
        text(map, "name", 255);
        String dataUrl = text(map, "dataUrl", Integer.MAX_VALUE);
        require(dataUrl.matches("(?s)^data:image/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=\\r\\n]+$"),
                "map.dataUrl 必须为 base64 图片");
        try {
            require(Base64.getMimeDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1)).length > 0,
                    "map.dataUrl 图片数据不能为空");
        } catch (IllegalArgumentException exception) {
            throw SpaceDocumentException.invalid("map.dataUrl 包含无效的 base64 数据");
        }
        number(map.path("x"), "map.x");
        number(map.path("y"), "map.y");
        positive(map.path("width"), "map.width");
        positive(map.path("depth"), "map.depth");
        nonNegative(map.path("thickness"), "map.thickness");
        opacity(map.path("opacity"), "map.opacity", true);
    }

    private Map<String, JsonNode> index(JsonNode snapshot, String collection, String idField) {
        JsonNode values = snapshot.path(collection);
        require(values.isArray(), collection + " 必须为数组");
        Map<String, JsonNode> indexed = new HashMap<>();
        for (JsonNode item : values) {
            object(item, collection + "[]");
            String id = text(item, idField, 128);
            require(indexed.put(id, item) == null, collection + " 中存在重复 " + idField + ": " + id);
        }
        return indexed;
    }

    private void point(JsonNode point, String path, double halfWidth, double halfDepth, double tolerance) {
        double x = number(point.path("x"), path + ".x");
        double y = number(point.path("y"), path + ".y");
        withinBounds(x, y, halfWidth, halfDepth, tolerance, path);
    }

    private void withinBounds(double x, double y, double halfWidth, double halfDepth, double tolerance, String path) {
        require(Double.isFinite(x) && Double.isFinite(y)
                        && Math.abs(x) <= halfWidth + tolerance && Math.abs(y) <= halfDepth + tolerance,
                path + " 超出空间边界，要求 |x| <= bounds.width/2 且 |y| <= bounds.depth/2");
    }

    private void object(JsonNode value, String path) {
        require(value != null && value.isObject(), path + " 必须为 JSON 对象");
    }

    private String text(JsonNode value, String field, int maxLength) {
        JsonNode node = value.path(field);
        require(node.isTextual() && StringUtils.hasText(node.textValue()) && node.textValue().length() <= maxLength,
                field + " 必须为非空字符串，长度不能超过 " + maxLength);
        return node.textValue();
    }

    private void string(JsonNode value, String path) {
        require(value.isTextual(), path + " 必须为字符串");
    }

    private void bool(JsonNode object, String field) {
        require(object.path(field).isBoolean(), field + " 必须为布尔值");
    }

    private void integer(JsonNode value, String path) {
        require(value.isIntegralNumber() && value.canConvertToInt(), path + " 必须为整数");
    }

    private double number(JsonNode value, String path) {
        require(value.isNumber() && Double.isFinite(value.doubleValue()), path + " 必须为有限数值");
        return value.doubleValue();
    }

    private double positive(JsonNode value, String path) {
        double result = number(value, path);
        require(result > 0, path + " 必须大于 0");
        return result;
    }

    private void nonNegative(JsonNode value, String path) {
        require(number(value, path) >= 0, path + " 不能小于 0");
    }

    private void opacity(JsonNode value, String path, boolean allowZero) {
        double opacity = number(value, path);
        require(opacity <= 1 && (allowZero ? opacity >= 0 : opacity > 0),
                path + (allowZero ? " 必须在 [0,1] 之间" : " 必须在 (0,1] 之间"));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw SpaceDocumentException.invalid(message);
        }
    }
}
