package com.neucity.common.core.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 汉字转拼音工具类（基于pinyin4j实现）
 *
 * @author ShuPF
 * @date 2026/2/2 14:16
 */
public final class Chinese2PinyinUtil {

    // 私有化构造方法，禁止实例化工具类
    private Chinese2PinyinUtil() {}

    /**
     * 初始化拼音输出格式（公共配置）
     * @param caseType 大小写格式（大写/小写）
     * @param toneType 声调格式（带声调/不带声调）
     * @return 拼音输出格式对象
     */
    private static HanyuPinyinOutputFormat initPinyinFormat(HanyuPinyinCaseType caseType, HanyuPinyinToneType toneType) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        // 设置大小写
        format.setCaseType(caseType);
        // 设置声调格式
        format.setToneType(toneType);
        // 无需设置声母格式（默认即可，如需区分v和ü可额外配置）
        return format;
    }

    /**
     * 汉字转全拼（不带声调，小写）
     * @param chinese 待转换的汉字字符串
     * @return 转换后的全拼字符串
     */
    public static String toFullPinyinLowerNoTone(String chinese) {
        return convertChinese2Pinyin(chinese,
                HanyuPinyinCaseType.LOWERCASE,
                HanyuPinyinToneType.WITHOUT_TONE,
                false);
    }

    /**
     * 汉字转全拼（不带声调，大写）
     * @param chinese 待转换的汉字字符串
     * @return 转换后的全拼字符串
     */
    public static String toFullPinyinUpperNoTone(String chinese) {
        return convertChinese2Pinyin(chinese,
                HanyuPinyinCaseType.UPPERCASE,
                HanyuPinyinToneType.WITHOUT_TONE,
                false);
    }

    /**
     * 汉字转全拼（带声调，小写）
     * @param chinese 待转换的汉字字符串
     * @return 转换后的全拼字符串
     */
    public static String toFullPinyinLowerWithTone(String chinese) {
        return convertChinese2Pinyin(chinese,
                HanyuPinyinCaseType.LOWERCASE,
                HanyuPinyinToneType.WITH_TONE_NUMBER,
                false);
    }

    /**
     * 提取汉字首字母（大写，不带声调）
     * @param chinese 待转换的汉字字符串
     * @return 转换后的首字母字符串
     */
    public static String toFirstLetterUpper(String chinese) {
        return convertChinese2Pinyin(chinese,
                HanyuPinyinCaseType.UPPERCASE,
                HanyuPinyinToneType.WITHOUT_TONE,
                true);
    }

    /**
     * 提取汉字首字母（小写，不带声调）
     * @param chinese 待转换的汉字字符串
     * @return 转换后的首字母字符串
     */
    public static String toFirstLetterLower(String chinese) {
        return convertChinese2Pinyin(chinese,
                HanyuPinyinCaseType.LOWERCASE,
                HanyuPinyinToneType.WITHOUT_TONE,
                true);
    }

    /**
     * 核心转换方法：处理汉字转拼音的具体逻辑
     * @param chinese 待转换的汉字字符串
     * @param caseType 大小写格式
     * @param toneType 声调格式
     * @param isFirstLetter 是否只提取首字母
     * @return 转换后的拼音字符串
     */
    private static String convertChinese2Pinyin(String chinese,
                                                HanyuPinyinCaseType caseType,
                                                HanyuPinyinToneType toneType,
                                                boolean isFirstLetter) {
        // 入参校验：为空直接返回空字符串
        if (chinese == null || chinese.trim().isEmpty()) {
            return "";
        }
        // 初始化字符串构建器，拼接结果
        StringBuilder pinyinSb = new StringBuilder();
        // 转换为字符数组，逐个处理
        char[] chineseChars = chinese.trim().toCharArray();
        HanyuPinyinOutputFormat format = initPinyinFormat(caseType, toneType);

        for (char ch : chineseChars) {
            // 判断是否为汉字（Unicode编码范围：0x4E00 - 0x9FA5）
            if (Character.toString(ch).matches("[\\u4E00-\\u9FA5]")) {
                try {
                    // 获取当前汉字的拼音数组（多音字会返回多个拼音，这里取第一个，如需处理多音字可额外扩展）
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        if (isFirstLetter) {
                            // 提取首字母
                            pinyinSb.append(pinyinArray[0].charAt(0));
                        } else {
                            // 拼接完整拼音
                            pinyinSb.append(pinyinArray[0]);
                        }
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 格式配置异常，捕获后不抛出，跳过当前字符（避免整体转换失败）
                    e.printStackTrace();
                }
            } else {
                // 非汉字字符（数字、字母、符号等），直接保留
                pinyinSb.append(ch);
            }
        }
        return pinyinSb.toString();
    }

    public static void main(String[] args) {
        System.out.println(toFullPinyinLowerNoTone("舒老师"));
    }

}
