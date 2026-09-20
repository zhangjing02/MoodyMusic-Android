package com.example.moodymusicforandroid.common.utils

import java.nio.charset.Charset

/**
 * 拼音首字母工具类（用于歌手、歌曲首字母分组索引）
 * 支持中文（GB2312汉字拼音快速映射与多音字/繁体特殊姓氏字典）、英文、数字与特殊字符
 */
object PinyinUtils {

    /**
     * 常用特殊姓氏与多音字首字母映射表
     */
    private val SPECIAL_SURNAMES: Map<Char, Char> = mapOf(
        // 多音字与特殊姓氏
        '重' to 'C', // 重新/重庆
        '区' to 'O', // 区(ōu)
        '朴' to 'P', // 朴(piáo)树
        '单' to 'S', // 单(shàn)
        '解' to 'X', // 解(xiè)
        '仇' to 'Q', // 仇(qiú)
        '查' to 'Z', // 查(zhā)
        '曾' to 'Z', // 曾(zēng)
        '盖' to 'G', // 盖(gě)
        '缪' to 'M', // 缪(miào)
        '繁' to 'P', // 繁(pó)
        '折' to 'S', // 折(shé)
        '乐' to 'Y', '樂' to 'Y', // 乐(yuè)
        '那' to 'N', // 那(nā/nà)英

        // 华语乐坛二级汉字/特殊姓氏补全（GB2312 扩展区）
        '庾' to 'Y', // 庾(yǔ)澄庆
        '窦' to 'D', '竇' to 'D', // 窦(dòu)唯
        '臧' to 'Z', // 臧(zāng)天朔
        '邰' to 'T', // 邰(tái)正宵
        '岑' to 'C', // 岑(cén)宁儿
        '裘' to 'Q', // 裘(qiú)德 / 裘海正
        '郁' to 'Y', // 郁(yù)可唯
        '邝' to 'K', '鄺' to 'K', // 邝(kuàng)美云
        '卞' to 'B', // 卞(biàn)留念
        '迟' to 'C', '遲' to 'C', // 迟(chí)志强
        '甄' to 'Z', // 甄(zhēn)妮
        '瞿' to 'Q', // 瞿(qú)颖
        '靳' to 'J', // 靳(jìn)东
        '郝' to 'H', // 郝(hǎo)云
        '鲍' to 'B', '鮑' to 'B', // 鲍(bào)家街
        '龚' to 'G', '龔' to 'G', // 龚(gōng)琳娜
        '阎' to 'Y', '閻' to 'Y', // 阎(yán)维文
        '欧' to 'O', '歐' to 'O', // 欧(ōu)阳
        '齐' to 'Q', '齊' to 'Q', // 齐(qí)秦
        '腾' to 'T', '騰' to 'T', // 腾(téng)格尔
        '艾' to 'A', // 艾(ài)敬
        '降' to 'J', // 降(jiàng)央卓玛
        '屠' to 'T', // 屠(tú)洪刚
        '杭' to 'H', // 杭(háng)天琪
        '尹' to 'Y', // 尹(yǐn)相杰
        '萨' to 'S', '薩' to 'S', // 萨(sà)顶顶
        '斯' to 'S', // 斯(sī)琴格日乐
        '央' to 'Y', // 央(yāng)吉玛
        '扎' to 'Z', // 扎(zhā)西顿珠
        '容' to 'R', // 容(róng)祖儿
        '谭' to 'T', '譚' to 'T', // 谭(tán)咏麟
        '潘' to 'P', // 潘(pān)玮柏
        '关' to 'G', '關' to 'G', // 关(guān)淑怡
        '卢' to 'L', '盧' to 'L', // 卢(lú)冠廷
        '罗' to 'L', '羅' to 'L', // 罗(luó)大佑
        '叶' to 'Y', '葉' to 'Y', // 叶(yè)倩文
        '钟' to 'Z', '鍾' to 'Z', // 钟(zhōng)汉良
        '梁' to 'L', // 梁(liáng)静茹
        '汪' to 'W', // 汪(wāng)峰
        '童' to 'T', // 童(tóng)安格
        '孟' to 'M', // 孟(mèng)庭苇
        '巫' to 'W', // 巫(wū)启贤
        '柯' to 'K', // 柯(kē)以敏
        '戴' to 'D', // 戴(dài)佩妮
        '姜' to 'J', // 姜(jiāng)育恒
        '熊' to 'X', // 熊(xióng)天平
        '苏' to 'S', '蘇' to 'S', // 苏(sū)芮
        '辛' to 'X', // 辛(xīn)晓琪
        '黎' to 'L', // 黎(lí)明
        '崔' to 'C', // 崔(cuī)健
        '刀' to 'D', // 刀(dāo)郎
        '伍' to 'W', // 伍(wǔ)佰
        '莫' to 'M', // 莫(mò)文蔚
        '费' to 'F', '費' to 'F', // 费(fèi)玉清
        '薛' to 'X', // 薛(xuē)之谦
        '蔡' to 'C', // 蔡(cài)琴/依林
        '韦' to 'W', '韋' to 'W', // 韦(wéi)唯
        '庞' to 'P', '龐' to 'P', // 庞(páng)龙

        // 繁简对照常用姓氏
        '张' to 'Z', '張' to 'Z',
        '陈' to 'C', '陳' to 'C',
        '刘' to 'L', '劉' to 'L',
        '黄' to 'H', '黃' to 'H',
        '赵' to 'Z', '趙' to 'Z',
        '吴' to 'W', '吳' to 'W',
        '孙' to 'S', '孫' to 'S',
        '杨' to 'Y', '楊' to 'Y',
        '郑' to 'Z', '鄭' to 'Z',
        '谢' to 'X', '謝' to 'X',
        '萧' to 'X', '蕭' to 'X',
        '冯' to 'F', '馮' to 'F',
        '邓' to 'D', '鄧' to 'D',
        '许' to 'X', '許' to 'X',
        '沈' to 'S',
        '贾' to 'J', '賈' to 'J'
    )

    private val GB2312_CHARSET: Charset by lazy {
        try {
            Charset.forName("GB2312")
        } catch (_: Exception) {
            Charset.defaultCharset()
        }
    }

    /**
     * 获取汉字或字符串的首字母大写 ('A'..'Z')，非字母返回 '#'
     */
    fun getPinyinInitial(str: String?): String {
        if (str.isNullOrBlank()) return "#"
        val trimmed = str.trim()
        val firstChar = trimmed.first()

        // 1. 英文字母直接转大写
        if (firstChar in 'a'..'z') return firstChar.uppercaseChar().toString()
        if (firstChar in 'A'..'Z') return firstChar.toString()

        // 2. ASCII 特殊字符/数字返回 '#'
        if (firstChar.code < 128) return "#"

        // 3. 特殊姓氏与繁体字快速匹配
        val special = SPECIAL_SURNAMES[firstChar]
        if (special != null) return special.toString()

        // 4. 利用 GB2312 编码一级常用汉字按拼音严格排序特性快速检索
        try {
            val bytes = firstChar.toString().toByteArray(GB2312_CHARSET)
            if (bytes.size >= 2) {
                val high = bytes[0].toInt() and 0xFF
                val low = bytes[1].toInt() and 0xFF
                val code = (high shl 8) + low

                val initial = when (code) {
                    in 0xB0A1..0xB0C4 -> 'A'
                    in 0xB0C5..0xB2C0 -> 'B'
                    in 0xB2C1..0xB4ED -> 'C'
                    in 0xB4EE..0xB6E9 -> 'D'
                    in 0xB6EA..0xB7A1 -> 'E'
                    in 0xB7A2..0xB8C0 -> 'F'
                    in 0xB8C1..0xB9FD -> 'G'
                    in 0xB9FE..0xBBF6 -> 'H'
                    in 0xBBF7..0xBFA5 -> 'J'
                    in 0xBFA6..0xC0AB -> 'K'
                    in 0xC0AC..0xC2E7 -> 'L'
                    in 0xC2E8..0xC4C2 -> 'M'
                    in 0xC4C3..0xC5B5 -> 'N'
                    in 0xC5B6..0xC5BD -> 'O'
                    in 0xC5BE..0xC6D9 -> 'P'
                    in 0xC6DA..0xC8BA -> 'Q'
                    in 0xC8BB..0xC8F5 -> 'R'
                    in 0xC8F6..0xCBF9 -> 'S'
                    in 0xCBFA..0xCDD9 -> 'T'
                    in 0xCDDA..0xCEF3 -> 'W'
                    in 0xCEF4..0xD1B8 -> 'X'
                    in 0xD1B9..0xD4D0 -> 'Y'
                    in 0xD4D1..0xD7F9 -> 'Z'
                    else -> '#'
                }
                if (initial != '#') return initial.toString()
            }
        } catch (_: Exception) {}

        return "#"
    }
}
