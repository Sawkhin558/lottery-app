package com.example.lotteryapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lotteryapp.data.LotteryDatabase
import com.example.lotteryapp.data.LotteryItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var db: LotteryDatabase
    private lateinit var inputEditText: EditText
    private lateinit var resultTextView: TextView
    private lateinit var summaryTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize database
        db = LotteryDatabase.getDatabase(this)
        
        // Initialize views
        inputEditText = findViewById(R.id.inputEditText)
        resultTextView = findViewById(R.id.resultTextView)
        summaryTextView = findViewById(R.id.summaryTextView)
        
        val processButton: Button = findViewById(R.id.processButton)
        val clearButton: Button = findViewById(R.id.clearButton)
        val showHistoryButton: Button = findViewById(R.id.showHistoryButton)
        val calculateButton: Button = findViewById(R.id.calculateButton)
        
        // Set up button click listeners
        processButton.setOnClickListener {
            val input = inputEditText.text.toString().trim()
            if (input.isNotEmpty()) {
                processInput(input)
            } else {
                Toast.makeText(this, "ကျေးဇူးပြု၍ စာရင်းထည့်ပါ", Toast.LENGTH_SHORT).show()
            }
        }
        
        clearButton.setOnClickListener {
            inputEditText.text.clear()
            resultTextView.text = ""
            Toast.makeText(this, "ရှင်းလင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
        }
        
        showHistoryButton.setOnClickListener {
            showVoucherHistory()
        }
        
        calculateButton.setOnClickListener {
            calculateProfit()
        }
        
        // Load initial summary
        updateSummary()
    }
    
    private fun processInput(input: String) {
        lifecycleScope.launch {
            try {
                val items = parseLotteryInput(input)
                if (items.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "မှားယွင်းနေသော format", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // Calculate total
                val total = items.sumOf { it.directAmount + it.rolledAmount }
                
                // Create voucher
                val voucher = com.example.lotteryapp.data.Voucher(
                    rawText = input,
                    totalAmount = total
                )
                
                // Insert into database
                val voucherId = db.voucherDao().insertVoucher(voucher).toInt()
                
                // Insert items
                items.forEach { item ->
                    item.voucherId = voucherId
                    db.lotteryItemDao().insertItem(item)
                }
                
                runOnUiThread {
                    val resultText = StringBuilder()
                    resultText.append("✅ စာရင်းသွင်းပြီးပါပြီ\n\n")
                    resultText.append("ဘောင်ချာအမှတ်: $voucherId\n")
                    resultText.append("စုစုပေါင်းတန်ဖိုး: $total ကျပ်\n\n")
                    resultText.append("အသေးစိတ်စာရင်း:\n")
                    
                    items.forEach { item ->
                        resultText.append("${item.number}: ဒဲ့=${item.directAmount}, တွတ်=${item.rolledAmount}\n")
                    }
                    
                    resultTextView.text = resultText.toString()
                    inputEditText.text.clear()
                    Toast.makeText(this@MainActivity, "စာရင်းသွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    
                    // Update summary
                    updateSummary()
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    resultTextView.text = "❌ အမှား: ${e.message}"
                    Toast.makeText(this@MainActivity, "အမှားရှိပါသည်: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun parseLotteryInput(text: String): List<LotteryItem> {
        val items = mutableListOf<LotteryItem>()
        val lines = text.split(' ').filter { it.trim().isNotEmpty() }
        
        for (line in lines) {
            if (!line.contains('=')) {
                throw IllegalArgumentException("Format error: No '=' in '$line'")
            }
            
            val parts = line.split('=')
            val numbers = parts[0].split('.').map { it.trim() }
            val valuePart = parts[1]
            
            for (number in numbers) {
                if (number.length != 3 || !number.all { it.isDigit() }) {
                    throw IllegalArgumentException("Number must be 3 digits: '$number'")
                }
            }
            
            when {
                valuePart.contains('*') -> {
                    // Format: 123=100*50
                    val values = valuePart.split('*')
                    val direct = values[0].toIntOrNull() ?: 0
                    val rolled = values.getOrNull(1)?.toIntOrNull() ?: 0
                    
                    numbers.forEach { number ->
                        items.add(LotteryItem(
                            number = number,
                            directAmount = direct,
                            rolledAmount = rolled,
                            type = if (rolled > 0) "rolled" else "direct"
                        ))
                    }
                }
                
                'r' in valuePart.lowercase() -> {
                    // Format: 123=100r50 or 123=r50
                    val cleanValue = valuePart.replace(Regex("[rR]"), "")
                    val amount = cleanValue.toIntOrNull() ?: 0
                    
                    numbers.forEach { number ->
                        items.add(LotteryItem(
                            number = number,
                            directAmount = amount,
                            rolledAmount = 0,
                            type = "direct"
                        ))
                    }
                }
                
                else -> {
                    // Format: 123=100
                    val amount = valuePart.toIntOrNull() ?: 0
                    numbers.forEach { number ->
                        items.add(LotteryItem(
                            number = number,
                            directAmount = amount,
                            rolledAmount = 0,
                            type = "direct"
                        ))
                    }
                }
            }
        }
        
        return items
    }
    
    private fun showVoucherHistory() {
        lifecycleScope.launch {
            val vouchers = db.voucherDao().getAllVouchers()
            val total = db.voucherDao().getTotalAmount() ?: 0
            val count = db.voucherDao().getVoucherCount()
            
            runOnUiThread {
                val historyText = StringBuilder()
                historyText.append("📊 ဘောင်ချာမှတ်တမ်း\n\n")
                historyText.append("စုစုပေါင်းဘောင်ချာ: $count ခု\n")
                historyText.append("စုစုပေါင်းတန်ဖိုး: $total ကျပ်\n\n")
                
                if (vouchers.isNotEmpty()) {
                    historyText.append("နောက်ဆုံး ၅ ခု:\n")
                    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    
                    vouchers.take(5).forEach { voucher ->
                        val time = dateFormat.format(Date(voucher.timestamp))
                        historyText.append("$time - ${voucher.totalAmount} ကျပ်\n")
                    }
                }
                
                resultTextView.text = historyText.toString()
            }
        }
    }
    
    private fun calculateProfit() {
        lifecycleScope.launch {
            val totalSales = db.voucherDao().getTotalAmount() ?: 0
            val commission = totalSales * 10 / 100  // 10% commission
            val estimatedPayout = totalSales * 50 / 100  // 50% estimated payout
            val netProfit = totalSales - commission - estimatedPayout
            
            runOnUiThread {
                val profitText = """
                    💰 အမြတ်အရှုံးခန့်မှန်းခြေ
                    
                    📊 စုစုပေါင်းရောင်းရငွေ: $totalSales ကျပ်
                    💸 ကော်မရှင် (၁၀%): $commission ကျပ်
                    🎲 ခန့်မှန်းလျော်ကြေး (၅၀%): $estimatedPayout ကျပ်
                    📈 ခန့်မှန်းအသားတင်: $netProfit ကျပ်
                    
                    ⚠️ မှတ်ချက်: ဤသည်မှာ ခန့်မှန်းခြေသာဖြစ်ပါသည်။
                    ပေါက်ဂဏန်းပေါ်မူတည်၍ ပြောင်းလဲနိုင်ပါသည်။
                """.trimIndent()
                
                resultTextView.text = profitText
            }
        }
    }
    
    private fun updateSummary() {
        lifecycleScope.launch {
            val total = db.voucherDao().getTotalAmount() ?: 0
            val count = db.voucherDao().getVoucherCount()
            
            runOnUiThread {
                summaryTextView.text = "ဘောင်ချာ: $count ခု | စုစုပေါင်း: $total ကျပ်"
            }
        }
    }
}