# 微信播报 · Android App

监听微信通知 → 用系统语音把消息读出来。原生 Android 应用，**装得上安卓 16**，有正规图标名字，**语音助手可以喊开**。

## 怎么把它变成能装的 APK（全程网页操作，不用装任何软件）

### 一、新建仓库
1. 打开 https://github.com ，右上角 **+** → **New repository**；
2. **Repository name** 填 `wx-speaker`；
3. 选 **Public**（公开，编译免费）或 Private 都行；
4. 点 **Create repository**。

### 二、上传所有文件
1. 进入刚建的仓库，点 **uploading an existing file**（或 **Add file → Upload files**）；
2. 把**本文件夹里的所有内容**（包括 `.github` 文件夹！）**拖进去**；
   - 注意是拖**里面的内容**，让仓库根目录直接是 `settings.gradle.kts`、`app`、`.github` 这些；
3. 下面 Commit changes 点 **Commit changes**。

> ⚠️ `.github` 文件夹**必须上传**，它就是自动编译的配置。拖文件夹时它会被一起带上去。

### 三、等它自动编译
1. 上传完成后，点仓库上方的 **Actions** 标签；
2. 会看到一条 **Build APK** 的任务在跑（黄色转圈 → 绿色打勾，约 2～5 分钟）；
3. 变成绿色 ✅ 后，点进去，页面底部 **Artifacts** 里下载 **wx-speaker-apk**（是个 zip，解压得到 `app-debug.apk`）。

### 四、安装到手机
1. 把 `app-debug.apk` 传到手机安装；
2. 打开 **微信播报** → 点 **① 开启「通知使用权」** → 在列表里找到「微信播报」打开 → 返回；
3. 状态会显示"已授权 ✅ / 已开启 ✅"。

### 五、使用
- **语音助手**说「**打开微信播报**」→ App 打开即开始播报；
- 微信来消息会被读出来；
- 想停：点 App 里的「停止播报」，或把 App 从后台划掉。

## 功能
- 文字消息 → 直接朗读
- 纯标点（如 `。。。`）→ 念"3个句号"
- 语音消息 → 念"收到一条语音消息"
- 图片/视频/表情包/文件/链接 → 念"收到一张图片"等
- 微信 `[N条]` 重复推送 → 自动去重
- 正文里已带发送人名字时 → 不重复念

## 想改配置
打开 `app/src/main/java/com/example/wxspeaker/NotifyListener.kt`，改开头的常量：
- `WECHAT_PKG`：微信包名
- `DEDUP_MS`：去重时间
- `MEDIA`：媒体提示语
改完 Commit，Actions 会重新编译。
