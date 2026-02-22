package com.example.lotteryapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
                        val items = allVoucherItems.filter { it.voucherId == v.id }.map { i ->
                            mapOf(
                                "n" to i.number,
                                "d" to i.directAmount,
                                "r" to i.rolledAmount,
                                "type" to i.type
                            )
                        }
                        mapOf(
                            "id" to v.id,
                            "time" to dateFormat.format(Date(v.timestamp)),
                            "rawText" to v.rawText,
                            "items" to items,
                            "total" to v.totalAmount
                        )
                    }
                    
                    // Load master history with items
                    val masterList = db.masterHistoryDao().getAllSync()
                    val allMasterItems = db.masterHistoryDao().getAllItemsSync()
                    val masterJson = masterList.map { m ->
                        val items = allMasterItems.filter { it.masterId == m.id }.map { i ->
                            mapOf(
                                "n" to i.number,
                                "d" to i.directAmount,
                                "r" to i.rolledAmount
                            )
                        }
                        mapOf(
                            "id" to m.id,
                            "time" to m.time,
                            "items" to items,
                            "total" to m.total
                        )
                    }
                    
                    val result = mapOf(
                        "vouchers" to vouchersJson,
                        "masterHistory" to masterJson
                    )
                    
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
                    val data = gson.fromJson<Map<String, Any>>(json, Map::class.java)
                    val db = database
                    
                    // Clear existing data
                    db.voucherDao().deleteAllVouchers()
                    db.lotteryItemDao().deleteAllItems()
                    db.masterHistoryDao().deleteAll()
                    db.masterHistoryDao().deleteAllItems()
                    
                    // Save vouchers
                    val vouchersList = (data["vouchers"] as? List<Map<String, Any>>) ?: emptyList()
                    vouchersList.forEach { vMap ->
                        val voucher = Voucher(
                            id = (vMap["id"] as? Number ?: 0).toLong(),
                            timestamp = System.currentTimeMillis(),
                            rawText = vMap["rawText"] as? String ?: "",
                            totalAmount = (vMap["total"] as? Number ?: 0).toInt()
                        )
                        val voucherId = db.voucherDao().insertVoucher(voucher).toInt()
                        
                        val itemsList = vMap["items"] as? List<Map<String, Any>> ?: emptyList()
                        itemsList.forEach { iMap ->
                            val item = LotteryItem(
                                voucherId = voucherId,
                                number = iMap["n"] as? String ?: "",
                                directAmount = (iMap["d"] as? Number ?: 0).toInt(),
                                rolledAmount = (iMap["r"] as? Number ?: 0).toInt(),
                                type = iMap["type"] as? String ?: "direct"
                            )
                            db.lotteryItemDao().insertItem(item)
                        }
                    }
                    
                    // Save master history
                    val masterList = (data["masterHistory"] as? List<Map<String, Any>>) ?: emptyList()
                    masterList.forEach { mMap ->
                        val master = MasterHistory(
                            id = (mMap["id"] as? Number ?: 0).toLong(),
                            time = mMap["time"] as? String ?: "",
                            total = (mMap["total"] as? Number ?: 0).toInt()
                        )
                        val masterId = db.masterHistoryDao().insert(master).toInt()
                        
                        val itemsList = mMap["items"] as? List<Map<String, Any>> ?: emptyList()
                        itemsList.forEach { iMap ->
                            val item = MasterItem(
                                masterId = masterId,
                                number = iMap["n"] as? String ?: "",
                                directAmount = (iMap["d"] as? Number ?: 0).toInt(),
                                rolledAmount = (iMap["r"] as? Number ?: 0).toInt()
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