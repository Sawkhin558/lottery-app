package com.example.lotteryapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data classes for JSON serialization
data class JsVoucher(
    val id: Long,
    val time: String,
    val rawText: String,
    val items: List<JsItem>,
    val total: Int
)

data class JsItem(
    val n: String,
    val d: Int,
    val r: Int,
    val type: String
)

data class JsMaster(
    val id: Long,
    val time: String,
    val items: List<JsItem>,
    val total: Int
)

data class JsData(
    val vouchers: List<JsVoucher>,
    val masterHistory: List<JsMaster>
)

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private val gson = Gson()
    private val dateFormat by lazy { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(AndroidBridge(), "AndroidBridge")
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/index.html")
        }
        
        setContentView(webView)
    }
    
    inner class AndroidBridge {
        
        private val database: LotteryDatabase
            get() = LotteryDatabase.getDatabase(applicationContext)
        
        @JavascriptInterface
        fun loadData() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = database
                    
                    // Load vouchers with items
                    val voucherList = db.voucherDao().getAllVouchersSync()
                    val allVoucherItems = db.lotteryItemDao().getAllItemsSync()
                    val vouchersJson = voucherList.map { v ->
                        val items = allVoucherItems
                            .filter { it.voucherId == v.id }
                            .map { i -> JsItem(i.number, i.directAmount, i.rolledAmount, i.type) }
                        JsVoucher(
                            id = v.id,
                            time = dateFormat.format(Date(v.timestamp)),
                            rawText = v.rawText,
                            items = items,
                            total = v.totalAmount
                        )
                    }
                    
                    // Load master history with items
                    val masterList = db.masterHistoryDao().getAllSync()
                    val allMasterItems = db.masterHistoryDao().getAllItemsSync()
                    val masterJson = masterList.map { m ->
                        val items = allMasterItems
                            .filter { it.masterId == m.id }
                            .map { i -> JsItem(i.number, i.directAmount, i.rolledAmount) }
                        JsMaster(
                            id = m.id,
                            time = m.time,
                            items = items,
                            total = m.total
                        )
                    }
                    
                    val result = JsData(vouchersJson, masterJson)
                    val json = gson.toJson(result)
                    
                    runOnUiThread {
                        webView.evaluateJavascript("window.onDataLoaded('${json.replace("'", "\\'")}')") {}
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        @JavascriptInterface
        fun saveVouchers(json: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data = gson.fromJson(json, JsData::class.java)
                    val db = database
                    
                    // Clear existing data
                    db.voucherDao().deleteAllVouchers()
                    db.lotteryItemDao().deleteAllItems()
                    db.masterHistoryDao().deleteAll()
                    db.masterHistoryDao().deleteAllItems()
                    
                    // Save vouchers
                    data.vouchers.forEach { v ->
                        val voucher = Voucher(
                            id = v.id,
                            timestamp = System.currentTimeMillis(),
                            rawText = v.rawText,
                            totalAmount = v.total
                        )
                        val voucherId = db.voucherDao().insertVoucher(voucher).toInt()
                        
                        v.items.forEach { i ->
                            val item = LotteryItem(
                                voucherId = voucherId,
                                number = i.n,
                                directAmount = i.d,
                                rolledAmount = i.r,
                                type = i.type
                            )
                            db.lotteryItemDao().insertItem(item)
                        }
                    }
                    
                    // Save master history
                    data.masterHistory.forEach { m ->
                        val master = MasterHistory(
                            id = m.id,
                            time = m.time,
                            total = m.total
                        )
                        val masterId = db.masterHistoryDao().insert(master).toInt()
                        
                        m.items.forEach { i ->
                            val item = MasterItem(
                                masterId = masterId,
                                number = i.n,
                                directAmount = i.d,
                                rolledAmount = i.r
                            )
                            db.masterHistoryDao().insertItem(item)
                        }
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        @JavascriptInterface
        fun clearAll() {
            CoroutineScope(Dispatchers.IO).launch {
                val db = database
                db.voucherDao().deleteAllVouchers()
                db.lotteryItemDao().deleteAllItems()
                db.masterHistoryDao().deleteAll()
                db.masterHistoryDao().deleteAllItems()
            }
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}