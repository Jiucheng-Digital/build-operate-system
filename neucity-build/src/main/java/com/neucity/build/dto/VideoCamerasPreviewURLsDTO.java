package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class VideoCamerasPreviewURLsDTO {
    @NotNull(message = "监控点唯一标识不能为空")
    private String cameraIndexCode;
    /**
     *  码流类型，0:主码流
     * 1:子码流
     * 2:第三码流
     */
    private Integer streamType = 0;
    /**
     *  取流协议（应用层协议）。 “hik”:HIK私有协议，使用视频SDK进行播放时，
     *  传入此类型；“rtsp”:RTSP协议；“rtmp”:RTMP协议；“hls”:HLS协议；“hlss”:基于ssl/tls的HLS协议；
     *  “ws”:Websocket协议；“wss”:基于ssl/tls的Websocket协议；“httpflv”:HTTP-FLV协议；
     *  “httpsflv”:基于ssl/tls的HTTP-FLV协议。 参数不填，默认为HIK协议，最大长度：64。
     *  注： 1、HLS协议只支持海康SDK协议、EHOME协议、ONVIF协议接入的设备；只支持H264、H265视频编码和AAC音频编码；
     *       2、Websocket协议仅限使用H5视频播放器取流播放 3、HTTP-FLV协议只支持海康SDK协议、EHOME协议、ONVIF协议接入的设备；
     *         只支持H264、h265视频编码和AAC音频编码。
     */
    private String protocol = "rtsp";
    /**
     *  传输协议（传输层协议），0:UDP
     * 1:TCP
     * 默认是TCP
     * 注：
     * GB28181 2011及以前版本只支持UDP传输
     */
    private Integer transmode = 1;
    /**
     *  标识扩展内容，格式：key=value，
     * 调用方根据其播放控件支持的解码格式选择相应的封装类型；
     */
    private String expand;
    /**
     *  输出码流转封装格式，“ps”:PS封装格式、“rtp”:RTP封装协议。当protocol=rtsp时生效，且不传值时默认为RTP封装协议。
     */
    private String streamform;

    /**
     *  驱动ID
     */
    @NotNull(message = "驱动ID不能为空")
    private Long driveId;
}
