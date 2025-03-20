package com.donut.tikdown.util

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.donut.tikdown.app
import com.donut.tikdown.appScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.userAgent
import io.ktor.serialization.gson.gson
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val mutex = Mutex()

@SuppressLint("SetJavaScriptEnabled")
suspend fun getVideoId(videoUrl: String): String {
    return withTimeout(1000 * 10) {
        mutex.withLock {
            suspendCancellableCoroutine { task ->
                genClient(app, task, videoUrl)
//        addComposeView {
//            AndroidView(factory = { context ->
//                genClient(context, task, videoUrl)
//            }, modifier = Modifier.systemBarsPadding())
//        }
            }
        }
    }
}

const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13; 22081212C Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/116.0.0.0 Mobile Safari/537.36 XWEB/1160043 MMWEBSDK/20231105 MMWEBID/4478 MicroMessenger/8.0.44.2502(0x28002C51) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64"

val client = HttpClient(CIO) {
    install(DefaultRequest) {
        userAgent(USER_AGENT)
    }
    install(ContentNegotiation) {
        gson()
    }
    install(HttpTimeout) {

    }
}

@OptIn(InternalCoroutinesApi::class)
suspend fun saveFileToStorage(
    url: String,
    displayName: String,
    progress: ProgressContent,
    directory: String = Environment.DIRECTORY_DOWNLOADS ,
    storeUri: Uri = MediaStore.Files.getContentUri("external"),
): Uri? {
    val resolver = app.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
//        put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "${directory}/TikDown")
    }


    val fileUri = resolver.insert(storeUri, contentValues)
    coroutineContext.job.invokeOnCompletion { throwable ->
        if (throwable !is CancellationException) {
            return@invokeOnCompletion
        }
        if (fileUri == null) {
            return@invokeOnCompletion
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            resolver.delete(fileUri, null)
        }
    }
    if (fileUri == null) {
        return null
    }
    client.prepareGet {
        timeout {
            requestTimeoutMillis = 1000 * 60 * 60
        }
        url(url)
        onDownload(progress.ktorListener)
    }.execute {
        resolver.openOutputStream(fileUri)?.use { output ->
            it.bodyAsChannel().toInputStream().use { input ->
                input.copyTo(output)
            }
        }
    }
    return fileUri
}

@SuppressLint("SetJavaScriptEnabled")
fun genClient(context: Context, task: CancellableContinuation<String>, videoUrl: String) =
    WebView(context).apply {
        val webView = this
        webViewClient = object : WebViewClient() {

            fun endClientWith(block: UnitBlock) {
                appScope.launch {
                    webView.destroy()
                }
                block()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val url = request?.url.toString()
                if (url.contains("share/note/")) {
                    endClientWith {
                        task.resumeWithException(Exception("不支持图文"))
                    }
                }
                if (url.contains("share/slides/")) {
                    endClientWith {
                        task.resumeWithException(Exception("不支持分段视频"))
                    }
                }
                return !(url.startsWith("http://") || url.startsWith("https://"))
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val url = request?.url.toString()
                if (url.contains("/captcha/")) {
                    endClientWith {
                        task.resumeWithException(Exception("出现验证码"))
                    }
                }
                if (url.contains("aweme-server-static-resource/reflow_notice_icon")) {
                    endClientWith {
                        task.resumeWithException(Exception("作品已失效"))
                    }
                }
                if (url.contains("video_id=")) {
                    endClientWith {
                        val videoId = url.substringAfter("video_id=").substringBefore("&")
                        task.resume(videoId)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
                super.onPageFinished(view, url)
            }
        }


        settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
            allowUniversalAccessFromFileURLs = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            userAgentString = USER_AGENT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }
        loadUrl(videoUrl)
    }

