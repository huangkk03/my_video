需求规格说明书：智能影院字幕全自动抓取与转码系统
1. 业务目标
系统应在视频入库或播放时，自动通过网络资源匹配、下载、解压、转压并挂载字幕，解决用户“手动找字幕、字幕乱码、格式不兼容”的痛点。

2. 核心逻辑流程 (Workflow)
触发阶段： 扫描到新视频或用户点击“在线搜索”。

搜索阶段： 后端根据 文件名/TMDB_ID 在第三方接口（如 OpenSubtitles）或爬虫站点（如字幕库）检索。

处理阶段： 下载压缩包 -> 解压 -> 编码检测 (Charset Detection) -> 格式转换 (SRT/ASS to VTT)。

展示阶段： Vue 前端通过 API 获取字幕 URL，注入播放器轨道。

3. 详细技术实现要求
A. 后端：字幕获取与处理 (Java)
① 字幕爬虫/搜索模块
搜索接口： 实现一个 SubtitleSearcher 接口，支持多源搜索。

模拟实现逻辑：

Java
// 使用 Jsoup 模拟抓取逻辑
Document doc = Jsoup.connect("https://xxx.com/search?q=" + movieName).get();
String downloadLink = doc.select(".download-link").first().attr("href");
② 压缩包处理 (Zip/Rar Handling)
功能： 很多字幕是打包的。使用 Apache Commons Compress 或 java.util.zip 实现自动解压。

过滤规则： 只保留 .srt, .ass, .ssa 文件，删除广告文本。

③ 编码纠正 (关键：解决中文乱码)
工具： 集成 icu4j 或 juniversalchardet。

逻辑： 1. 检测原始文件编码（如 GBK, BIG5）。
2. 如果不是 UTF-8，则读取内容并重新以 UTF-8 写入临时文件。

④ FFmpeg 格式转换 (HLS/Web 兼容)
命令逻辑： 浏览器对 WebVTT 支持最好。

FFmpeg 命令：

Bash
# 强制将字幕转换为 vtt 格式，并处理时间戳
ffmpeg -i input.srt -y output.vtt
B. 前端：Vue 3 动态加载 (Vue + ArtPlayer)
API 对接： 调用 GET /api/media/{id}/subtitles。

动态注入：

JavaScript
// ArtPlayer 实例配置
subtitle: {
    url: currentSubUrl,
    type: 'vtt',
    style: { color: '#fff', fontSize: '20px', textShadow: '0 0 2px #000' },
}
4. 给 OpenCode 的核心代码参考 (Methodology)
请 OpenCode 重点实现以下两个核心方法：

方法一：编码检测与转换 (Encoding Service)
Java
public void convertToUtf8Vtt(File sourceSub, File targetVtt) {
    // 1. 检测编码 (使用 icu4j)
    // 2. 以检测到的编码读取 String
    // 3. 调用 FFmpeg 转码：
    // ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", sourceSub.getAbsolutePath(), "-y", targetVtt.getAbsolutePath());
}
方法二：文件关联算法 (Naming Convention)
匹配规则： 搜索同级目录下包含 video_filename 关键字的所有文件。

重命名规范： 视频名.语言.vtt（例如：Peppa.Pig.S01E01.zh-cn.vtt）。

5. 验收清单 (Definition of Done)
[ ] 后端能成功从指定 URL 下载并解压出字幕。

[ ] GBK 编码的 SRT 字幕在浏览器播放时无乱码。

[ ] 11GB 的大文件播放时，外挂字幕同步无延迟。

[ ] 前端播放器控制栏出现“字幕选择”菜单，且能自由切换不。


“针对字幕获取，请帮我构建一个‘多级回退’的抓取策略：

第一优先级 (精准)： 调用 OpenSubtitles.com API。通过视频文件的 Hash 值请求字幕，这样能保证 11GB 大文件的字幕进度绝对同步。

第二优先级 (中文优化)： 调用 射手网 (Shooter) API，专门用于补全中文字幕轨道。

第三优先级 (兜底)： 编写一个基于 Jsoup 的轻量爬虫，抓取 字幕库 (Zimuku) 的搜索结果。

请先为我实现一个 SubtitleProvider 接口，并完成 OpenSubtitles 的 API 对接部分。”


API 频率限制： OpenSubtitles 对免费账户有下载次数限制（每天 5-10 个左右），建议你在 Java 端做一个 本地字幕缓存（存放在持久化卷中），同一个视频抓过一次就不要再抓第二次了。

Hash 计算： 11GB 的文件千万不要算全量 MD5，那会耗尽 IO！OpenSubtitles 有专门的 Hash 算法，只需读取文件头尾各 64KB，非常快。记得让 OpenCode 照着这个逻辑写。


