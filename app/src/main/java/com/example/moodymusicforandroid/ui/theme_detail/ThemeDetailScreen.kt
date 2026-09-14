package com.example.moodymusicforandroid.ui.theme_detail

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 现代颂歌 主题音乐详情页 (ThemeDetailScreen)
 *
 * 采用杂志专栏深度长文排版（大图摄影、文艺引语、物理黑胶叙事与居中播放操作卡）。
 * 用户在此页面阅读专题背景，点击播放卡启动全局播放器与悬浮播放窗。
 */
/**
 * 乐章时序关键节点解析数据模型
 */
private data class TimelineSection(
    val timeLabel: String,
    val title: String,
    val sceneStory: String,
    val emotion: String,
    val technique: String,
    val performerNote: String = ""
)

private data class ThemeStory(
    val issueTag: String,
    val categoryTag: String,
    val headline: String,
    val subtitle: String,
    val authorDate: String,
    val bodyParagraphs: List<String>,
    val quoteEn: String,
    val quoteZh: String,
    val scenariosTitle: String,
    val scenarios: List<String>,
    val benefitsTitle: String,
    val benefits: List<String>,
    val aboutTitle: String,
    val aboutDesc: String,
    val aboutMotto: String,
    val footerSign: String,
    val playPillTextActive: String,
    val playPillTextIdle: String,
    val isSquareCover: Boolean,
    val posterAspectRatio: Float = 16f / 10f,
    val timelineTitle: String = "🎼 乐章时序全景图解 · 剧情 / 情绪 / 演奏技巧剖析",
    val timelineSections: List<TimelineSection> = emptyList()
)

private fun resolveThemeStory(themeId: String): ThemeStory {
    return when (themeId) {
        "pop_piano_theme" -> ThemeStory(
            issueTag = "精选胶片 · ISSUE #83",
            categoryTag = "本 週 專 題 · 華 語 經 典 琴 聲",
            headline = "《華語流行情歌鋼琴曲》",
            subtitle = "只想靜靜聽音樂 · 抒情鋼琴曲 · 舒壓輕音樂 Relaxing Piano Music",
            authorDate = "Love Piano · 流行鋼琴   |   2026 特輯選刊",
            bodyParagraphs = listOf(
                "歡迎來到 Love Piano —— 在這裡，沒有語言的束縛，只有純淨動人的鋼琴旋律，輕輕流淌進你的心靈。\n\n每一段音符，都是一段故事；每一首旋律，都是一種情緒。無論你是在放鬆、學習、工作，還是獨處的夜晚，都希望能用溫柔的音樂陪伴你，帶來寧靜與治癒。",
                "閉上眼睛，讓黑白琴鍵的敲擊聲帶你遠離白日的喧囂，進入屬於自己的安靜世界。\n把時間交給琴鍵的律動，讓這些熟悉的流行旋律化作最溫暖的陪伴。"
            ),
            quoteEn = "Close your eyes, let the piano melodies take you away from the noise, and enter your own quiet world.\nLet music be the warmest companion in your life.",
            quoteZh = "“閉上眼睛，讓鋼琴的聲音帶你遠離喧囂，進入屬於自己的安靜世界。\n讓音樂成為你生活中最溫暖的陪伴。”",
            scenariosTitle = "🎧 適用場景 · Perfect for",
            scenarios = listOf(
                "• 華語經典流行金曲伴讀 / 舒緩背景音樂",
                "• 深夜獨處放鬆、撫平情緒、減壓冥想",
                "• 辦公專注、寫作創作、靈感流淌",
                "• 睡前靜心、改善睡眠品質",
                "• 閱讀、手帳、咖啡時光"
            ),
            benefitsTitle = "🎧 聽流行鋼琴的好處？",
            benefits = listOf(
                "✔  經典旋律化作純琴聲，不帶歌詞干擾，專注加倍",
                "✔  溫暖柔和的琴鍵共鳴，深層舒緩焦慮與日常壓力",
                "✔  平穩抒情的節奏律動，引導身心回歸寧靜放鬆",
                "✔  營造典雅溫馨的生活空間氛圍"
            ),
            aboutTitle = "👉 關於頻道｜About Love Piano",
            aboutDesc = "歡迎來到 Love Piano —— 一個充滿流行鋼琴、放鬆氛圍與慢節奏時光的音樂空間，致力於為大家帶來純淨治癒的器樂作品。",
            aboutMotto = "如果你也喜愛這樣的旋律，歡迎在任何需要溫暖陪伴時回來。\n\n— Just listen, relax, and heal.",
            footerSign = "Love Piano · 流行鋼琴特輯",
            playPillTextActive = "钢琴曲播放中",
            playPillTextIdle = "播放钢琴曲",
            isSquareCover = true
        )

        "jonathan_lee_theme" -> ThemeStory(
            issueTag = "大師傳世 · ISSUE #84",
            categoryTag = "本 週 專 題 · 華 語 音 樂 教 父",
            headline = "《李宗盛：年少不聽李宗盛，聽懂已是不惑年》",
            subtitle = "30首歲月金曲 · 寫盡人世間的悲歡離合、執著與釋懷",
            authorDate = "李宗盛 Jonathan Lee · 傳奇唱作人   |   經典傳世特輯",
            bodyParagraphs = listOf(
                "有人說，每個人心裡都住著一首李宗盛。\n\n不是因為他的旋律有多複雜，恰恰相反——他的音樂簡單到像你坐在對面的老友，用一種近乎白話的方式，替你說出你這輩子說不清楚的那些話。那些關於愛情的執念、關於青春的莽撞、關於中年之後才慢慢懂得的那種釋懷。",
                "他為林憶蓮寫下《當愛已成往事》，為陳淑樺寫下《夢醒時分》，為辛曉琪寫下《領悟》，為張信哲寫下《愛如潮水》。他是那個站在幕後、替別人說話最動人的那個人。\n\n直到後來他站到台前，唱起《凡人歌》，唱起《山丘》，你才知道——他其實一直都在替自己說話。",
                "《山丘》是他五十多歲才寫出的作品，那是一個男人走過半生風霜之後，站在山頭回望的輕嘆。“越過山丘，才發現無人等候；喋喋不休，再也喚不回溫柔。”沒有哭腔，沒有怒氣，只有一種看透之後的平靜——那種平靜，比任何吶喊都更令人動容。\n\n而在音樂之外，他還做起了手工吉他。Lee Guitars 的每一把木吉他，都是他親手刨木、組裝、打磨。他說：吉他比人誠實，你給它多少，它還給你多少。這句話，像極了他的人生觀。"
            ),
            quoteEn = "Having crossed the hill, only to find no one waiting;\nChattering endlessly, yet no longer able to recall that gentle tenderness.",
            quoteZh = "\u201c越過山丘，才發現無人等候；喋喋不休，再也喚不回溫柔。\n想得卻不可得，你奈人生何；該捨的捨不得，只顧著跟往事瞎扯。\u201d",
            scenariosTitle = "🎧 適用場景 · Perfect for",
            scenarios = listOf(
                "• 深夜獨處微醺，回顧半生往事與心境沉澱",
                "• 漫長高速公路長途駕駛，車窗外的風與心頭的歌",
                "• 繁重工作之余的片刻喘息，找回內心的平靜與韌性",
                "• 週末午後泡壺老茶或手衝咖啡，細品人生百味",
                "• 歷經風雨後的釋懷、自省與自愈時光"
            ),
            benefitsTitle = "🎧 為什麼要聽李宗盛？",
            benefits = listOf(
                "✔  直擊心靈的人間清醒：句句切中要害，撕開偽裝卻又溫柔包紮",
                "✔  手工吉他的木質溫度：Lee Guitars 的淳厚顆粒感音色，像冬日的熱茶",
                "✔  敘事般的半念半唱：沒有炫技與造作，只有歲月沉澱後的故事",
                "✔  三十首金曲連貫沉浸：兩個多小時的時光膠囊，一次聽盡一代傳奇"
            ),
            aboutTitle = "👉 關於李宗盛｜About Jonathan Lee",
            aboutDesc = "李宗盛，1958年生於台北。詞曲作者、音樂製作人、手工吉他製琴師。從《生命中的精靈》到《山丘》，橫跨三十餘年，金曲獎大滿貫。他為林憶蓮、陳淑樺、張信哲、莫文蔚、辛曉琪等人量身寫歌，塑造了一個時代的華語流行音樂面貌。晚年創立 Lee Guitars，用木頭與琴弦延續對音樂最純粹的熱愛。",
            aboutMotto = "願你歷盡千帆，歸來依然懂得感動。\n\n— 就這樣邊走邊唱，才算真的活過。",
            footerSign = "李宗盛 Jonathan Lee · 傳世經典特輯",
            playPillTextActive = "经典30首播放中",
            playPillTextIdle = "播放李宗盛经典30首",
            isSquareCover = true
        )

        "lofi_chill_theme" -> ThemeStory(
            issueTag = "大腦降噪 · ISSUE #85",
            categoryTag = "本 週 專 題 · 偷 得 浮 生 半 日 閒",
            headline = "《好聽到忘記時間的放鬆音樂》",
            subtitle = "Lo-fi Chill 溫柔旋律陪你慢慢回血 · 隔絕喧囂的治癒伴讀時光",
            authorDate = "Lova Radio · 治癒系音樂   |   2026 特輯選刊",
            bodyParagraphs = listOf(
                "在快節奏的生活中按下暫停鍵，讓這份「好聽到忘記時間」的 Lo-fi Chill 帶你進入一個沒有壓力的平行時空。\n\n深沉微妙的貝斯底音與溫潤的木吉他撥弦完美契合，伴隨著彷彿在耳邊私語的溫暖吟唱。每一次舒緩的節奏起伏，都在慢慢融化緊繃的神經，讓快樂的多巴胺緩緩釋放。",
                "這是一組能讓人徹底卸下防備、越聽越放鬆的治癒歌單。\n\n無論是窩在角落安靜閱讀、發呆放空，還是單純想給自己一段與世隔絕的休息時光，這組自帶安全感的音樂都能溫柔包覆你，讓你在不知不覺中忘記時間，慢慢回血。"
            ),
            quoteEn = "Disconnect from the noise, reconnect with your inner peace.\nLet time slow down and gently restore your soul.",
            quoteZh = "“暫時切斷外界的喧囂，在溫柔旋律中找回內心的平靜。\n把時間放慢，讓心靈悄悄回血。”",
            scenariosTitle = "🎧 適用場景 · Perfect for",
            scenarios = listOf(
                "• 渴望逃離繁忙步調，需要一段「忘記時間」來徹底放空與休息",
                "• 偏愛輕柔節奏與治癒氛圍，追求極致放鬆與情緒舒緩",
                "• 窩在陽光角落安靜閱讀、寫作手帳、沉浸式工作學習",
                "• 大腦降噪、隔絕雜訊、撫平焦慮日常",
                "• 睡前溫柔陪伴、營造鬆弛私密空間"
            ),
            benefitsTitle = "✨ 這份歌單的亮點？",
            benefits = listOf(
                "✔  充滿包覆感的原聲樂器與呢喃人聲，打造漸入佳境、讓人忘記時間的療癒體驗",
                "✔  深沉低音與溫潤吉他雙重共鳴，促發快樂多巴胺分泌",
                "✔  全程沉浸不被打斷，陪伴你度過每一個需要平靜的時刻",
                "✔  隨時按下暫停，偷得浮生半日閒，帶來最純粹的快樂"
            ),
            aboutTitle = "💌 關於頻道｜About Lova Radio",
            aboutDesc = "歡迎來到 Lova Radio —— 一個充滿 Lo-fi Chill、原聲吉他、放鬆氛圍與慢節奏時光的治癒空間，陪伴你專注、放空、閱讀與回血。",
            aboutMotto = "願這些充滿多巴胺的治癒音符，能為你偷得浮生半日閒，帶來最純粹的平靜。\n\n— Breathe in calm, breathe out noise.",
            footerSign = "Lova Radio · 治癒系 Lo-fi Chill",
            playPillTextActive = "治愈旋律播放中",
            playPillTextIdle = "播放 Lo-fi 旋律",
            isSquareCover = true
        )

        "bach_cello_theme" -> ThemeStory(
            issueTag = "巴洛克圣经 · ISSUE #93",
            categoryTag = "本 週 專 題 · 靈 魂 降 噪 · 巴 赫 大 提 琴",
            headline = "《用巴赫抚平浮躁：大提琴与钢琴的宁静之境》",
            subtitle = "全长 59 分钟纯真声演绎 · 沉浸于巴赫大提琴组曲与经典圣咏的冥想世界",
            authorDate = "Johann Sebastian Bach 作曲   |   Lu Dimon（大提琴） & Mu Dimon（钢琴） 纯真声录制",
            bodyParagraphs = listOf(
                "【让巴赫安抚一切噪音 · Let Bach quiet the noise】\n当世界充斥着信息过载与无休止的焦虑杂音，巴赫的大提琴声就像一汪清冽甘甜的林间泉水，沉静而有力地涤荡着心神。大提琴（Cello）拥有最接近人类声带音域的木质共鸣箱，其低沉浑厚的琴弦振动，能够瞬间降低呼吸频率，抚平紧绷的神经。\n\n本专辑由大提琴家 Lu Dimon（@Cellofeggio）与钢琴家 Mu Dimon（@MuDimon, @ClassicalofBirdland）联袂实况录制。全场 59 分钟坚持纯真声演奏——无任何电子合成循环（No loops）、无采样拼贴（No samples）、无人工智能生成（No AI）——每一个音符都出自琴弦与键盘在真实物理空间中的呼吸、揉弦与指尖触感，纯真纯粹。",
                "【巴赫大提琴作品的灵魂哲学】\n约翰·塞巴斯蒂安·巴赫（Johann Sebastian Bach，1685–1750）在柯滕时期（1717–1723）创作的六首《无伴奏大提琴组曲》（BWV 1007-1012），被后世誉为古典弓弦乐器的‘旧约圣经’。这些乐曲不仅是对大提琴演奏技法的极限探索，更是人类精神面对永恒、孤独与崇高时的哲学沉思。\n\n在本特辑中，演奏家们不仅精心呈现了第一、第二与第三大提琴组曲中最为深邃的《萨拉班德舞曲》（Sarabande）与《阿勒曼德舞曲》（Allemande），更穿插了巴赫流传百世的室内乐与键盘杰作——包括被誉为‘天国安抚之声’的《G弦上的咏叹调》（BWV 1068）、虔诚澄澈的《耶稣，世人仰望的喜悦》（BWV 147）、哀婉悠长的《F小调咏叹调》（BWV 1056）以及温婉明快的《G大调小步舞曲》（BWV Anh. 114）。",
                "【大提琴与钢琴的极简室内乐对话】\n不同于纯粹无伴奏的孤傲清绝，本辑中大提琴与钢琴的交融宛如两位挚友在静夜壁炉旁的低语。钢琴的柔和触键勾勒出巴洛克复调和声的骨架，大提琴如歌般的长弓线条则在和声织体之上自由流淌，在静谧中构筑出一座坚实而温和的心灵庇护所。"
            ),
            quoteEn = "Let Bach quiet the noise. Real strings, real touch, real peace.\nEvery single note played by hand, by real musicians. No loops, no AI.",
            quoteZh = "“让巴赫抚平身边的喧嚣与浮躁。真实的琴弦振动，指尖触碰的温度，唤醒内心最真实的宁静。\n在这近一个小时的时间里，让每一个纯真声演奏的音符带你安然呼吸。”",
            scenariosTitle = "🎧 适用场景 · Perfect for",
            scenarios = listOf(
                "• 深度工作与专注学习：去除歌词干扰，巴洛克严谨复调逻辑激发专注心流",
                "• 深夜独处、冥想静心：大提琴温暖低频如厚毯包裹身心，抚平焦虑与日常疲惫",
                "• 睡前助眠与神经放松：舒缓的心率同步节奏，引导身心步入深度放松",
                "• 伴读书写与灵感创作：温润典雅的木质原声，营造古典沙龙般的专注氛围",
                "• 瑜伽、茶歇与松弛时刻：放慢匆忙步伐，找回平稳悠长的呼吸节奏"
            ),
            benefitsTitle = "✨ 为什么巴赫大提琴具有奇迹般的疗愈力？",
            benefits = listOf(
                "✔  100% 纯真实录：拒绝 AI 生成与机械循环，保留琴弓触弦的呼吸感与箱体木质共鸣",
                "✔  巴洛克数学美感与和声神圣感：巴赫严密的对位法结构，科学证实能有效降低皮质醇水平",
                "✔  近乎人声的歌唱性：大提琴中低音频段最契合人类心脏搏动与呼吸起伏",
                "✔  一小时连贯音乐旅程：18 首经典曲目层层递进，从深邃组曲到明朗舞曲与崇高圣咏"
            ),
            aboutTitle = "👉 关于主创与演奏家｜About Artists",
            aboutDesc = "大提琴家 Lu Dimon（@Cellofeggio）与钢琴家 Mu Dimon（@MuDimon / @ClassicalofBirdland）专注于古典真声室内乐演绎，致力于用手作般的乐器实录与纯正古典曲目，为现代都市人创造抵御数字噪音与快餐文化的精神绿洲。",
            aboutMotto = "“如果你也因这琴声而感到呼吸稍微轻松了一些，那便是音乐最美好的意义。”\n\n— Real Strings. Real Peace.",
            footerSign = "Johann Sebastian Bach · Lu Dimon & Mu Dimon   |   巴赫大提琴作品集",
            playPillTextActive = "巴赫大提琴演奏中 (59:36)",
            playPillTextIdle = "聆听巴赫大提琴完整集 (59:36)",
            isSquareCover = false,
            posterAspectRatio = 16f / 9f,
            timelineTitle = "🎼 全辑 18 首曲目时序导赏 · 乐章解析与精神图解",
            timelineSections = listOf(
                TimelineSection(
                    timeLabel = "00:02 - 03:09",
                    title = "01. 萨拉班德舞曲，第一组曲 (Sarabande Suite I)",
                    sceneStory = "选自《G大调第一无伴奏大提琴组曲》BWV 1007。萨拉班德原为源自西班牙的三拍子庄严舞曲，在巴赫笔下化为崇高的灵魂独白。",
                    emotion = "静穆端庄、沉思内省、神圣肃穆",
                    technique = "慢板 3/4 拍，重音落在第二拍。大提琴运用宽阔深沉的下弓拉奏，双音和弦勾勒出立体多声部错觉，开篇即带来强烈的降噪与宁静感。"
                ),
                TimelineSection(
                    timeLabel = "03:09 - 05:26",
                    title = "02. 圣咏 (Choral)",
                    sceneStory = "巴赫崇高虔敬的圣咏旋律。钢琴柔和铺陈和声背景，大提琴如圣咏人声般缓缓吟唱。",
                    emotion = "虔诚圣洁、慰藉抚平、温暖澄澈",
                    technique = "如歌的连弓（Legato cantabile），音色柔和圆润，气韵绵长，如同冬日晨曦洒入教堂彩窗。"
                ),
                TimelineSection(
                    timeLabel = "05:26 - 07:13",
                    title = "03. G大调小步舞曲 (Minuet in G Major, BWV Anh. 114)",
                    sceneStory = "出自《安娜·玛格达莱娜·巴赫的笔记本》，巴赫家族最为人熟知、明快典雅的宫廷舞曲。",
                    emotion = "典雅轻快、明朗舒展、温馨愉悦",
                    technique = "中庸的 3/4 拍小步舞曲节奏，琴弓起落轻盈灵动，左右手指尖触弦颗粒分明，洋溢着巴洛克家庭沙龙的温情。"
                ),
                TimelineSection(
                    timeLabel = "07:13 - 11:21",
                    title = "04. F小调第五号羽管键琴协奏曲：咏叹调 (Arioso, BWV 1056)",
                    sceneStory = "巴赫最著名的哀婉旋律之一。被改编为大提琴与钢琴二重奏后，大提琴深沉的质感将作品的悲悯与释怀演绎得淋漓尽致。",
                    emotion = "深情幽微、哀而不伤、如泣如诉、释然开阔",
                    technique = "长弓慢运，大提琴在中低把位展现极为浓郁的天鹅绒般音质，细致的揉弦（Vibrato）与细腻的强弱呼吸（Dynamics）层次分明。"
                ),
                TimelineSection(
                    timeLabel = "11:21 - 14:48",
                    title = "05. D大调第三号管弦乐组曲：咏叹调 (Air from Suite No. 3, BWV 1068)",
                    sceneStory = "举世闻名的《G弦上的咏叹调》原曲。管弦组曲中的第二乐章，被后世小提琴家威廉密改编后名扬天下，本版由大提琴低回倾诉。",
                    emotion = "天国纯净、永恒超脱、洗净铅华、浩瀚安详",
                    technique = "如履平川的长线条旋律，钢琴稳健的八分音符低音步态（Walking Bass）与大提琴空灵悠扬的悬留音相互依附，达到音画合一的神圣意境。"
                ),
                TimelineSection(
                    timeLabel = "14:48 - 17:36",
                    title = "06. 若你与我相伴 (Bist Du Bei Mir, BWV 508)",
                    sceneStory = "巴赫写给挚爱妻子安娜的深情赞歌：‘若你伴我身旁，我便能安然走向安息与永恒’。",
                    emotion = "坚贞深情、安然托付、恬静纯美",
                    technique = "朴素真挚的歌唱性旋律，没有炫技与繁复织体，每一声下弓都宛如深情执手的诺言。"
                ),
                TimelineSection(
                    timeLabel = "17:36 - 20:10",
                    title = "07. F大调小步舞曲 (Minuet in F Major, BWV Anh. 113)",
                    sceneStory = "同样出自巴赫笔记本的精致舞曲，调性转为温暖明快的 F 大调。",
                    emotion = "轻柔优雅、从容舒缓、惬意灵动",
                    technique = "大提琴在中高音区呈现柔和通透的银铃般音色，跳动的短弓与流畅的滑音相得益彰。"
                ),
                TimelineSection(
                    timeLabel = "20:10 - 24:54",
                    title = "08. 阿勒曼德舞曲，第一组曲 (Allemande Suite I, BWV 1007)",
                    sceneStory = "第一无伴奏组曲的核心舞曲，4/4 拍中速德意志舞曲，充满了严密的逻辑线条与内生动力。",
                    emotion = "从容端庄、逻辑严谨、步履坚定",
                    technique = "绵延不断的十六分音符分解和弦，弓法平稳连贯，大提琴单声部织体暗含丰富的复调对位，展现巴赫高超的‘隐伏声部’（Implied Polyphony）。"
                ),
                TimelineSection(
                    timeLabel = "24:54 - 30:54",
                    title = "09. 萨拉班德舞曲，第二组曲 (Sarabande Suite II, BWV 1008)",
                    sceneStory = "选自《D小调第二无伴奏大提琴组曲》。D小调在巴赫笔下总是带着深刻的悲剧力量与悲悯叹息。",
                    emotion = "苍凉沉郁、孤高深邃、灵魂拷问",
                    technique = "阴郁沉缓的 D 小调和声，大提琴大量运用低把位深压弦与双音停留，音色饱含岁月的沧桑感与厚重感。"
                ),
                TimelineSection(
                    timeLabel = "30:54 - 34:31",
                    title = "10. 小步舞曲，第二组曲 (Minuets I & II, Suite II, BWV 1008)",
                    sceneStory = "第二组曲中的插舞。小步舞曲 I 为 D 小调的幽暗端庄，小步舞曲 II 转为 D 大调的微光破晓，形成对比鲜明的明暗互映。",
                    emotion = "幽微转折、暗香浮动、破晓微光",
                    technique = "大小调色彩的微妙转换，弓法细腻克制，低音共鸣充沛。"
                ),
                TimelineSection(
                    timeLabel = "34:31 - 38:11",
                    title = "11. 阿勒曼德舞曲，第二组曲 (Allemande Suite II, BWV 1008)",
                    sceneStory = "D小调第二组曲的阿勒曼德舞曲，相比第一组曲更添几分沉思与哲理意味。",
                    emotion = "静默沉潜、步履徐徐、思接千载",
                    technique = "连绵起伏的下行音型，揉弦幅度克制微小，保留巴洛克时期直率古朴的‘肠弦’质感。"
                ),
                TimelineSection(
                    timeLabel = "38:11 - 42:24",
                    title = "12. 萨拉班德舞曲，第三组曲 (Sarabande Suite III, BWV 1009)",
                    sceneStory = "选自《C大调第三无伴奏大提琴组曲》。C大调如同正午阳光般饱满明亮，是整套组曲中最具英雄气概与开阔胸襟的一部。",
                    emotion = "宏大开阔、崇高庄严、光明博大",
                    technique = "饱满有力的全弓拉奏，大提琴发挥粗弦的丰厚共鸣，三拍子重音沉稳如磐石。"
                ),
                TimelineSection(
                    timeLabel = "42:24 - 46:27",
                    title = "13. 阿勒曼德舞曲，第三组曲 (Allemande Suite III, BWV 1009)",
                    sceneStory = "C大调第三组曲的阿勒曼德舞曲，旋律如阳光下波光粼粼的溪流，生机盎然。",
                    emotion = "开朗豁达、欢欣畅快、生生不息",
                    technique = "流畅自然的弓位转换，清晰跳跃的分解和弦，大提琴箱体共鸣充盈通透。"
                ),
                TimelineSection(
                    timeLabel = "46:27 - 49:47",
                    title = "14. 耶稣，世人仰望的喜悦 (Cantata BWV 147)",
                    sceneStory = "巴赫教会康塔塔第 147 号中最广为人知的合唱乐章。9/8 拍的流线型三连音织体，象征着神圣恩典如甘霖普降人间。",
                    emotion = "无上喜悦、宁静祥和、慈悲洗礼、心满意足",
                    technique = "钢琴持续流淌的三连音流水织体，托起大提琴绵长庄严的赞美诗主题，双乐器交织出令人屏息的精神圣境。"
                ),
                TimelineSection(
                    timeLabel = "49:47 - 52:23",
                    title = "15. 前奏曲，第一组曲 (Prelude Suite I, BWV 1007)",
                    sceneStory = "大提琴史上最经典的‘开山之作’。由分解和弦构筑的永动机般流淌，被无数电影、文学作品奉为宁静与专注的最高象征。",
                    emotion = "纯澈明晰、周而复始、生生不息、宁静心流",
                    technique = "经典的单音分解和弦，左右手极度均匀的律动，每一次换把都展现出绝对的圆润与自然，让大脑完全沉浸于专注状态。"
                ),
                TimelineSection(
                    timeLabel = "52:23 - 55:30",
                    title = "16. 萨拉班德，第一组曲（回响篇）(Sarabande Suite I Reprise)",
                    sceneStory = "首部乐曲的再次回归。如同旅人在阅尽千帆之后重新回到最初的起点，心境却已截然不同。",
                    emotion = "释怀淡泊、回甘悠长、洗尽铅华",
                    technique = "更为轻柔微弱的弱奏（Pianissimo），琴音近乎低语，带来抚平一切焦躁的静谧力量。"
                ),
                TimelineSection(
                    timeLabel = "55:30 - 57:47",
                    title = "17. 圣咏（晚祷篇）(Choral Evening Prayer)",
                    sceneStory = "黄昏降临、万籁俱寂时的圣咏重现。琴声渐远，为疲惫的心灵披上一件温暖的外衣。",
                    emotion = "安宁沉静、晚祷抚慰、身心合一",
                    technique = "极长时值的连音运弓，音色深沉绵密，低音弦缓慢衰减，引导呼吸渐趋深长。"
                ),
                TimelineSection(
                    timeLabel = "57:47 - 59:36",
                    title = "18. G大调小步舞曲（终章致意）(Minuet in G Major, BWV Anh. 114 Finale)",
                    sceneStory = "全辑终曲。在温馨明快的小步舞曲中，演奏家们向听众致意告别，留下长久回荡的宁静与喜乐。",
                    emotion = "温暖告别、心旷神怡、豁然开朗、余韵悠长",
                    technique = "轻盈舒展的舞步节奏，尾声大提琴与钢琴在清澈纯净的 G 大调主和弦上渐慢弱化，余音袅袅，归于宁静。"
                )
            )
        )

        "butterfly_lovers_deep_dive" -> ThemeStory(
            issueTag = "东方交响 · ISSUE #92",
            categoryTag = "本 週 專 題 · 經 典 傳 世 名 作 剖 析",
            headline = "《梁祝》小提琴协奏曲",
            subtitle = "以西方交响之弓，引越剧缠绵之韵 · 独奏家宋知垣现场导赏与 21 分钟纯音史诗",
            authorDate = "何占豪 & 陈钢 作曲   |   宋知垣（小提琴） & 韩蓓琳（钢琴） 海菲茨音乐学院实况",
            bodyParagraphs = listOf(
                "1959 年，上海音乐学院两位才华横溢的年轻学子——26 岁出身越剧团的小提琴学生何占豪，与 24 岁谙熟西方和声的作曲系学生陈钢，携手创作了震惊中外乐坛的《梁山伯与祝英台》小提琴协奏曲。\n\n这部作品不仅是中国交响乐历史上演率最高、流传最广的民族交响丰碑，更是‘西洋乐器中国化、民族戏曲交响化’的开山典范。它以单乐章奏鸣曲式为骨架，将西方小提琴的华彩双音、跳音与中国越剧尹派（梁山伯）、袁派（祝英台）唱腔的连弓滑音、揉弦紧密熔铸于一体，生动再现了一段跨越千年的凄美爱情传奇。",
                "【独奏家现场导赏翻译 · 宋知垣 (Ji-Won Song)】\n在海菲茨国际音乐学院（Heifetz Institute）现场，独奏家宋知垣在演奏前手持小提琴向世界听众细致拆解了全曲的脉络：\n\n“这是一部在中国家喻户晓的爱情传说。在中国古代，女子没有受教育的权利，祝英台女扮男装远赴杭城求学，路上与梁山伯结拜为兄弟并坠入爱河，但憨直的山伯却始终以为她是个男孩。整部协奏曲严密跟随剧情的情感与场景展开——开篇阳光明媚，是充满希望的新一天；紧接着你能从小提琴急促不安的颤动中，听出英台内心小鹿乱撞、想向山伯表白女儿身却又害羞的少女忐忑；随后快板跳音（Spiccato）惟妙惟肖地描绘出两人在书院庭院嬉戏捉迷藏、无忧无虑的同窗岁月……\n\n直到长亭送别，英台借湖面倒影与双飞游鹅不断暗示，山伯却始终未解其意；英台归家后，严父逼嫁太守之子，小提琴以刚烈叛逆的切分音与尖锐双音与封建强权展开殊死搏斗；楼台相会时山伯得知真相却为时已晚，如泣如诉的悲声成为全曲最心碎的一幕；山伯相思病逝后英台哭灵投坟，狂风骤雨、墓穴轰然裂开，英台纵身跳入；最后两人化蝶重生，最初的爱情主题在空灵清澈的泛音中升华永恒。\n\n而在全曲的最末尾，小提琴奏出几近微弱的空灵余韵，宛如祝英台在千年之后的回眸轻诉：这整部传奇，都是我一生的故事。希望大家能随我们的琴声一同走进这段史诗。”",
                "【曲式架构与戏曲交响化】\n作品宏观上遵循经典的西方单乐章奏鸣曲式，同时创新地以中国越剧唱腔发展逻辑为主线，严格契合民间传说的三大矛盾戏剧冲突：呈示部（相爱）、展开部（抗婚）、再现部（化蝶）。\n\n在小提琴演奏法上，不仅拓展了高把位自然泛音、近琴马演奏（sul ponticello）等西洋技术，更大胆引入中国民间拉弦乐器（二胡、板胡）独特的‘滑音加装饰音’与戏曲‘紧拉慢唱’，真正实现了‘以琴代声、人琴合一’的至高境界。"
            ),
            quoteEn = "At the very end of this piece, you will hear a faint whisper from the violin. This is the girl saying: this whole story was actually my story, and she was the narrator of eternity.",
            quoteZh = "“在全曲的尾声，小提琴如同一抹微弱的叹息回响。那是祝英台在轻声诉说：这千回百转的爱恨与风雨，其实都是我一生的故事，而我便是这段永恒传奇的讲述者。”",
            scenariosTitle = "🎧 适用场景 · Perfect for",
            scenarios = listOf(
                "• 沉浸式古典音乐鉴赏，细品中西交响融合的殿堂级美学",
                "• 伴随下方 7 大时间节点边听边读，精准把握剧情、情绪与演奏技巧",
                "• 深夜独处静心聆听，感受东方古典爱情的极致浪漫与凄美绝唱",
                "• 音乐学习与小提琴技法研习，领略越剧唱腔移植与紧拉慢唱精髓",
                "• 情绪释放与心灵疗愈，在 21 分钟的化蝶史诗中找回内心的纯粹与感动"
            ),
            benefitsTitle = "✨ 为什么这部《梁祝》现场演奏版不可错过？",
            benefits = listOf(
                "✔  独家 21 分 23 秒纯音乐版：剔除开场英文解说，保留完整的现场琴弦呼吸与情感共振",
                "✔  小提琴与钢琴的极简对话：剥离庞大管弦织体，更凸显独奏小提琴细腻润腔与颗粒感音色",
                "✔  越剧唱腔的教科书级呈现：连弓滑音（Portamento）、柔美揉弦与活泼跳音转换自然天成",
                "✔  紧拉慢唱戏剧张力：展开部哭灵投坟的高潮处理扣人心弦、震撼肺腑",
                "✔  空灵透明的化蝶泛音：尾声高把位纯净泛音，如彩蝶羽翼破茧成仙、升华永恒"
            ),
            aboutTitle = "👉 关于主创与演奏家｜About Artists",
            aboutDesc = "何占豪、陈钢于 1959 年创作本曲。本录音由韩国知名青年小提琴家宋知垣（Ji-Won Song，曾荣获利平斯基-维尼亚夫斯基国际小提琴比赛、利奥波德·莫扎特国际小提琴比赛金奖）与中国旅美青年钢琴家韩蓓琳（Beilin Han）于海菲茨国际音乐学院（Heifetz Institute）现场携手呈现。",
            aboutMotto = "“愿天下有情人终成眷属，纵然生死殊途，亦可化蝶相依。”\n\n— 谨以东方交响之韵，致敬不朽真爱。",
            footerSign = "何占豪 & 陈钢 · 宋知垣 & 韩蓓琳   |   东方交响名作特辑",
            playPillTextActive = "《梁祝》演奏中 (21:23)",
            playPillTextIdle = "聆听《梁祝》完整演奏 (21:23)",
            isSquareCover = false,
            posterAspectRatio = 3f / 4f,
            timelineTitle = "🎼 乐章时序全景图解 · 剧情 / 情绪 / 演奏技巧剖析",
            timelineSections = listOf(
                TimelineSection(
                    timeLabel = "00:00 - 03:46",
                    title = "I. 呈示部 · 草桥结拜与爱情主题 (Adagio cantabile)",
                    sceneStory = "春光明媚，鸟语花香。祝英台女扮男装远赴杭城求学，草桥亭畔与同路书生梁山伯萍水相逢，一见如故，草桥结拜为异姓金兰。",
                    emotion = "纯真明朗、春意盎然、诗意温存、心心相印",
                    technique = "柔板（Adagio cantabile）。钢琴流水琶音模拟晨曦鸟鸣；小提琴以越剧尹派/袁派唱腔为基础，大量运用纯正中国传统民族风格的连弓滑音（Portamento）与细密揉弦，音色温润内敛；随后与钢琴低音声部对答互诉，如同两人携手并肩漫步。",
                    performerNote = "独奏家宋知垣示范：‘乐曲开篇非常晴朗，阳光明媚，是充满希望的新一天。接着你能从小提琴急促不安的颤音中，听出英台想向山伯表白女儿身却又害羞的少女忐忑。’"
                ),
                TimelineSection(
                    timeLabel = "03:46 - 06:23",
                    title = "II. 呈示部 · 同窗共读与华彩嬉戏 (Allegro)",
                    sceneStory = "万松书院，同窗三载，朝夕相处。课间在庭院花丛中追逐嬉闹、捉迷藏、扑蝶，少男少女间萌生纯洁无暇的情愫。",
                    emotion = "活泼诙谐、灵动欢快、朝气蓬勃、无忧无虑",
                    technique = "快板（Allegro）。小提琴运用弓根轻巧跳音（Spiccato）、紧凑回旋音（Turn）与活泼琶音，惟妙惟肖地模仿少年追逐跳跃的步态；中段插入温婉抒情副题，细致描摹英台心生倾慕却欲语还休的娇羞神态。",
                    performerNote = "独奏家宋知垣示范：‘这一段非常轻快有趣，小提琴用轻巧跳音描绘了他们在学校捉迷藏的童真场景，充满着无忧无虑的快乐。’"
                ),
                TimelineSection(
                    timeLabel = "06:23 - 08:29",
                    title = "III. 呈示部 · 十八相送与长亭惜别 (Adagio assai doloroso)",
                    sceneStory = "三年期满，英台奉命返家。山伯十八里长亭相送，依依惜别。英台一路借水池倒影、双飞游鹅频频暗示女儿身，山伯憨厚未解其意，惜别情深。",
                    emotion = "缠绵悱恻、依依不舍、深情款款、幽微怅惘",
                    technique = "极慢板（Adagio assai）。旋律深度借鉴中国二胡、板胡等弓弦乐器的拉弦运指风格，运用悠长延展的气息与大幅度单音滑奏，下行音调如叹息般回荡，刻画长亭送别时的万般眷恋与前途未卜的隐忧。",
                    performerNote = "独奏家宋知垣示范：‘英台指着倒影说“一男一女”，山伯较真说“明明是两个男生”；英台又指着鹅说“一公一母”，山伯反驳说“你怎么知道”。山伯太憨厚了，英台始终没能挑明心意，旋律充满了无奈与深情。’"
                ),
                TimelineSection(
                    timeLabel = "08:29 - 11:22",
                    title = "IV. 展开部 · 逼嫁抗婚与严酷冲突 (Pesante - Piu mosso - Duramente)",
                    sceneStory = "英台归家后，惊悉父亲已强行将其许配给马太守之子马文才。英台宁死不从，面对封建宗族与父权的残酷压迫，誓死抗婚！",
                    emotion = "阴森压抑、剑拔弩张、风云突变、激烈反抗",
                    technique = "展开部核心冲突。钢琴猛烈强奏阴冷沉重的低音和弦（Pesante 沉重），象征不可撼动的封建礼教桎梏；小提琴则以强烈的切分节奏（Syncopation）、尖锐刺耳的快速双音（Double stops）与大跳刚烈强音（Duramente）破空反击，形成极具戏剧张力的殊死交锋。",
                    performerNote = "独奏家宋知垣示范：‘父亲逼婚，英台极为愤怒并决意反抗，你能听到充满叛逆与怒火的强烈双音与切分重音！’"
                ),
                TimelineSection(
                    timeLabel = "11:22 - 14:46",
                    title = "V. 展开部 · 楼台相会与山伯郁终 (Lagrimoso)",
                    sceneStory = "山伯闻讯赶赴祝家庄楼台相会，得知英台已被逼订婚，两人泪眼相对、互诉衷肠。山伯归去后心力交瘁、相思成疾，溘然长逝。",
                    emotion = "撕心裂肺、万念俱灰、如泣如诉、悲怆凄婉",
                    technique = "悲泣慢板（Lagrimoso）。小提琴与钢琴展开深情如泣的双声部对答，深度吸收越剧戏曲“尺调腔”哀婉哭腔的润腔精髓，弓弦摩擦如杜鹃啼血。结尾处音乐节奏骤降、音响消退，宣示山伯亡故。",
                    performerNote = "独奏家宋知垣示范：‘山伯终于知道英台原来是个女孩，但一切都太晚了……这是整部协奏曲中最心碎、最悲伤的时刻。随后几小节内山伯病逝。’"
                ),
                TimelineSection(
                    timeLabel = "14:46 - 16:33",
                    title = "VI. 展开部尾声 · 哭灵投坟与天崩地裂 (Presto resoluto / Cadenza)",
                    sceneStory = "出嫁之日，英台素服扑向山伯墓前哀恸哭灵。刹那间狂风怒吼、雷电撕裂苍穹，坟墓轰然裂开，英台义无反顾纵身跃入墓穴！",
                    emotion = "悲愤狂暴、天地动容、义无反顾、壮烈殉情",
                    technique = "急板华彩（Presto resoluto / Cadenza）。巧妙化用京剧与越剧著名的“倒板”与“紧拉慢唱”绝技——伴奏以极速猛烈的连续颤音与沉重击键模拟风雨大作、天崩地裂，独奏小提琴则在极度自由的散板与大跨度滑音中凄厉长啸，情感张力迸发至全曲巅峰！最后巨响，墓穴复合。",
                    performerNote = "独奏家宋知垣示范：‘英台在墓前痛哭，突然坟墓裂开，她毫不犹豫跳入墓中！紧拉慢唱技巧将剧情推向狂暴顶点。’"
                ),
                TimelineSection(
                    timeLabel = "16:33 - 21:23",
                    title = "VII. 再现部与尾声 · 破茧成蝶与升华永恒 (Adagio cantabile)",
                    sceneStory = "风暴骤止，云开日出，彩虹横跨天际。梁山伯与祝英台的灵魂破茧成蝶，于漫天仙境繁花中蹁跹起舞，生死相依，升华永恒。",
                    emotion = "超凡脱俗、空灵剔透、圣洁宁谧、永恒救赎",
                    technique = "再现部与尾声。钢琴流水般的轻柔琶音重现仙境序幕。小提琴在高把位再次奏响经典的爱情主题，并创新融入晶莹空灵的自然泛音（Harmonics）与微颤音（Tremolo），仿佛彩蝶在金色阳光中轻盈展翅。乐曲在极度平静微弱的泛音余韵中渐渐消逝。",
                    performerNote = "独奏家宋知垣示范：‘在最末尾，小提琴最后几个微弱音符，仿佛英台在说：“这其实是我自己的故事，我是全篇传奇的讲述者。”’"
                )
            )
        )

        else -> ThemeStory(
            issueTag = "深度专题 · ISSUE #82",
            categoryTag = "本 週 專 題 · 慢 節 奏 時 光",
            headline = "《雪天咖啡館的閱讀鋼琴》",
            subtitle = "窗邊熱咖啡、一本書，慢慢過今天 Relaxing Piano Music",
            authorDate = "小葉 Let's Pause Together · 療癒鋼琴   |   2026 冬季選刊",
            bodyParagraphs = listOf(
                "窗外的雪安安靜靜地下著，屋裡有暖燈、壁爐，還有桌上剛剛好的那杯咖啡。\n翻開一本書，讓鋼琴聲在空氣裡輕輕流動，整個人也會跟著慢下來。",
                "很適合讀書、放空、整理思緒，或者只是想找一段不被催促的時間。\n\n就把今天暫時放慢一點，和雪景、咖啡，還有音樂一起安靜待著吧。"
            ),
            quoteEn = "Pause, breathe, and soften your day.\nTake a gentle moment to return to calm and balance.",
            quoteZh = "“放慢一下，深呼吸，讓一天變得柔和。\n讓自己歇息片刻，回到安靜與平衡。”",
            scenariosTitle = "🎧 適用場景 · Perfect for",
            scenarios = listOf(
                "• Study music / 讀書與專注",
                "• Deep focus & productivity / 深度專注與工作效率",
                "• Relaxing music & stress relief / 舒緩壓力與焦慮",
                "• Sleep music & insomnia help / 睡眠音樂與改善失眠",
                "• Background music for work & reading / 工作與閱讀背景音樂"
            ),
            benefitsTitle = "🎧 聽療癒鋼琴的好處？",
            benefits = listOf(
                "✔  提升專注力與效率",
                "✔  舒緩壓力與焦慮",
                "✔  改善睡眠品質",
                "✔  營造安靜療癒氛圍"
            ),
            aboutTitle = "👉 關於頻道｜About this channel",
            aboutDesc = "歡迎來到小葉的 Let's Pause Together —— 一個充滿療癒鋼琴、放鬆氛圍與慢節奏時光的空間，幫助你專注、放鬆、讀書與入睡。",
            aboutMotto = "如果你感到放鬆，歡迎在任何需要片刻休息時回來。\n\n— Just pause. Breathe. And begin again.",
            footerSign = "小葉 Let's Pause Together · 慢調特輯",
            playPillTextActive = "伴读音乐播放中",
            playPillTextIdle = "播放伴读音乐",
            isSquareCover = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailScreen(
    themeId: String,
    title: String,
    audioUrl: String,
    coverUrl: String,
    artistName: String,
    isPlaying: Boolean,
    isThisThemeActive: Boolean,  // 当前播放器中播放的是此曲（不管播放/暂停）
    isMiniPlayerVisible: Boolean, // 悬浮播放窗是否显示（有任何音乐被加载）
    onBackClick: () -> Unit,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val story = remember(themeId) { resolveThemeStory(themeId) }

    androidx.activity.compose.BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "THE MODERN SONGBOOK",
                            style = MaterialTheme.typography.labelSmall,
                            color = SongbookColors.BurntOrange,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = story.issueTag,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 160.dp // 预留底部悬浮迷你播放窗高度
            )
        ) {
            // 1. 专题主视觉海报图 (支持轻触播放伴读原声)
            item(key = "theme_hero_image") {
                val fallback = when (themeId) {
                    "butterfly_lovers_deep_dive" -> R.drawable.hero_butterfly_lovers
                    "bach_cello_theme" -> R.drawable.hero_bach_cello
                    "jonathan_lee_theme" -> R.drawable.album_jonathan_lee
                    "pop_piano_theme" -> R.drawable.album_pop_piano
                    "lofi_chill_theme" -> R.drawable.album_lofi_chill
                    else -> R.drawable.home_vinyl_banner
                }
                val heroUrl = when (themeId) {
                    "bach_cello_theme" -> "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/hero/bach_cello_hero.jpg"
                    else -> coverUrl.ifBlank { fallback }
                }
                val aspect = if (story.isSquareCover) (1f) else story.posterAspectRatio

                // 封面播放交互逻辑：
                // 1. 若当前此曲正在播放 (isPlaying)，右下角显示动效 EQ 柱状图，轻触可暂停；
                // 2. 若当前此曲未在播放（未播放、已暂停、或因弱网/错误停滞），右下角显示三角播放图标，轻触封面立即触发播放或重新加载重试；
                // 彻底避免出现因一次加载失败导致封面被移除点击事件的假死死锁。
                val isCurrentlyPlayingThis = isThisThemeActive && isPlaying

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onPlayToggle() }
                ) {
                    SongbookImage(
                        model = heroUrl,
                        contentDescription = title,
                        fallbackRes = fallback,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 封面右下角：若正在播放伴读原声，展示无背景纯橘色跳动 EQ 柱状动画
                    if (isCurrentlyPlayingThis) {
                        CoverEqIndicator(
                            isAnimating = true,
                            barColor = SongbookColors.BurntOrangeLight,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(width = 22.dp, height = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }



            // 2. 刊头标题与元信息
            item(key = "theme_header_text") {
                // 仅当底部播放器尚未加载此曲时显示上方播放按钮；
                // 一旦加载到播放器中（无论当前正在播放还是已暂停），底部悬浮窗已有完整控制逻辑，上方播放按钮自动隐藏避免重复
                val showPlayButton = !isThisThemeActive && audioUrl.isNotBlank()

                // 封面图下面紧跟着的第一行文本，右侧与封面对齐并微内缩的小巧播放按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = story.categoryTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = SongbookColors.TerracottaBrown,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (showPlayButton) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(SongbookColors.BurntOrange.copy(alpha = 0.08f))
                                .border(1.dp, SongbookColors.BurntOrange.copy(alpha = 0.75f), CircleShape)
                                .clickable { onPlayToggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow_rounded),
                                contentDescription = "播放伴读原声",
                                tint = SongbookColors.BurntOrange,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = story.headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = SongbookColors.BurntOrange,

                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = story.authorDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. 杂志长文正文
            item(key = "theme_article_body") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    story.bodyParagraphs.forEachIndexed { idx, p ->
                        Text(
                            text = p,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (idx == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 28.sp
                        )
                    }

                    // 杂志拉页金句引言（双语对照）
                    Surface(
                        color = Color(0x33EDD9C0),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            SongbookColors.TerracottaBrown.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = story.quoteEn,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = SongbookColors.TerracottaBrown,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = story.quoteZh,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = SongbookColors.TerracottaBrown,
                                lineHeight = 26.sp
                            )
                        }
                    }

                    // 4. 乐章全景时序解析 (剧情 · 情绪 · 演奏技巧剖析)
                    if (story.timelineSections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = story.timelineTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SongbookColors.BurntOrange
                        )

                        story.timelineSections.forEachIndexed { idx, section ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    SongbookColors.TerracottaBrown.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 节点时间与序号
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = SongbookColors.TerracottaBrown,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = section.timeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = "SECTION 0${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SongbookColors.Outline,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        thickness = 0.5.dp
                                    )

                                    // 剧情场景
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "📖 剧情：",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SongbookColors.BurntOrange
                                        )
                                        Text(
                                            text = section.sceneStory,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // 情绪意境
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "🎭 情绪：",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SongbookColors.TerracottaBrown
                                        )
                                        Text(
                                            text = section.emotion,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // 技巧与配器
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "🎻 技巧：",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SongbookColors.BurntOrange
                                        )
                                        Text(
                                            text = section.technique,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // 独奏家现场导赏点睛
                                    if (section.performerNote.isNotBlank()) {
                                        Surface(
                                            color = SongbookColors.TerracottaBrown.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp)
                                            ) {
                                                Text(
                                                    text = "💡 导赏：",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SongbookColors.TerracottaBrown
                                                )
                                                Text(
                                                    text = section.performerNote,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontStyle = FontStyle.Italic,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 19.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 适用场景
                    Text(
                        text = story.scenariosTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.BurntOrange
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            story.scenarios.forEach { s ->
                                Text(
                                    text = s,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 好处
                    Text(
                        text = story.benefitsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.BurntOrange
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            story.benefits.forEach { b ->
                                Text(
                                    text = b,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 频道结语
                    Surface(
                        color = Color(0x247A4A28),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = story.aboutTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SongbookColors.TerracottaBrown
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = story.aboutDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = story.aboutMotto,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = SongbookColors.TerracottaBrown,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = story.footerSign,
                        style = MaterialTheme.typography.labelMedium,
                        color = SongbookColors.Outline,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}


/**
 * 低调三角播放按钮（保留备用）
 */
@Composable
private fun CoverPlayButton(
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(R.drawable.ic_play_arrow),
        contentDescription = "播放",
        tint = SongbookColors.BurntOrangeLight,
        modifier = modifier.size(26.dp)
    )
}

/**
 * EQ 柱状图播放指示器（状态2/3：此曲在播放器中时显示）
 *
 * - isAnimating = true（播放中）：三根柱子高度无限循环动画
 * - isAnimating = false（暂停）：三根柱子静止在中间高度
 */
@Composable
fun CoverEqIndicator(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = SongbookColors.BurntOrangeLight
) {
    val barCount = 3


    // 每根柱子的独立动画相位（错开，制造自然波浪效果）
    val infiniteTransition = rememberInfiniteTransition(label = "EqBars")

    val heights = (0 until barCount).map { index ->
        val animatedHeight by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + index * 120,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "EqBar$index"
        )
        if (isAnimating) animatedHeight else 0.45f
    }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = totalWidth / (barCount * 2 - 1)
        val barGap = barWidth

        for (i in 0 until barCount) {
            val barHeight = totalHeight * heights[i]
            val left = i * (barWidth + barGap)
            val top = totalHeight - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
