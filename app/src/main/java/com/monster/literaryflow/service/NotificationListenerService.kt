package com.monster.literaryflow.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.monster.literaryflow.Const.Companion.TvPackage
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.utils.OrderUtils
import com.monster.literaryflow.utils.SPUtils
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class NotificationListenerService : NotificationListenerService() {
    val symbol = "PEPEUSDT"
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListenerService", "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NotificationListenerService", "onListenerDisconnected")
        requestRebind(ComponentName(this, NotificationListenerService::class.java))
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (TvPackage != sbn?.packageName) {
            return
        }
        Log.d("NotificationListenerService", "onNotificationPosted")
        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras?.getString(Notification.EXTRA_TITLE, "") ?: ""

        if (!TextUtils.isEmpty(title) && title.contains("警报")) {
            handleNotification(extras.getString(Notification.EXTRA_TEXT, ""))
        }
        cancelAllNotifications()
    }


    // Function to set take profit
//    fun setTakeProfit(orderId: String, takeProfitPrice: Double) {
//        val params = mutableMapOf<String, Any>(
//            "symbol" to symbol,
//            "side" to "SELL",
//            "type" to "TAKE_PROFIT_MARKET",
//            "stopPrice" to takeProfitPrice,
//            "quantity" to 0.01
//        )
//        MyApp.binanceApi!!.createTrade().newOrder(params)
//    }

    private fun handleNotification(content: String) {
        val gson = Gson()
        val orderListJson = SPUtils[this, "orderList", ""] as String
        val type = object : TypeToken<MutableList<String>>() {}.type
        val list: MutableList<String> = gson.fromJson(orderListJson, type) ?: mutableListOf()
        var const = TimeUtils.getNowTime() + content


        GlobalScope.launch {
            withContext(Dispatchers.IO) {



                val parameters: MutableMap<String, Any> = LinkedHashMap()
//                var quantity = OrderUtils.getMarginBalance("PEPE")
//                Log.d("PEPE余额", quantity)

                if ( content.contains("买价") ){
                    // 找到"买价@"和"止盈@"的位置
                    val buyPriceStartIndex = content.indexOf("买价@") + 3 // "买价@"的长度为3
                    val takeProfitStartIndex = content.indexOf("止盈@") + 3 // "止盈@"的长度为3

                    // 截取买价和止盈价
                    val buyPriceEndIndex = content.indexOf("止盈@") // "止盈@"的位置
                    var buyPrice = content.substring(buyPriceStartIndex, buyPriceEndIndex).toFloat()

                    var takeProfitPrice = content.substring(takeProfitStartIndex).toFloat()
                    if (buyPrice>0 && buyPrice<1){
                        buyPrice *= 1000
                    }
                    if (takeProfitPrice>0 && takeProfitPrice<1){
                        takeProfitPrice *= 1000
                    }
                    const += " $content"
                    OrderUtils.checkAndUpdateOrders(buyPrice, takeProfitPrice)
                }

/*
                when {
                    content.contains("止盈限价") -> {
                        var sellPrice = content.substringAfter("止盈限价@")
                        sellPrice = String.format("%.8f", sellPrice.toFloat())
                        if (quantity.toFloat() < 1) {
                            const += " 无货币，不执行"
                            Log.e("NotificationListenerService", "无货币，不执行")
                        } else {
                            if (MyApp.sellOrderId != 0L) {
                                val params: MutableMap<String, Any> = LinkedHashMap()
                                params.apply {
                                    put("symbol", "PEPEUSDT")
                                    put("orderId", MyApp.sellOrderId.toString())
                                }
                                try {
                                    MyApp.binanceApi!!.createMargin().cancelOpenOrders(params)
                                    MyApp.sellOrderId = 0L
                                } catch (_: Exception) {
                                }
                            }
                            const += " 发起卖单，价格:$sellPrice"
                            parameters.apply {
                                put("symbol", "PEPEUSDT")
                                put("side", "SELL")
                                put("type", "LIMIT")
                                put("timeInForce", "GTC")
                                put("price", sellPrice)
                                put("quantity", quantity.toFloat().toLong().toString())
                            }
                            try {
                                val result = MyApp.binanceApi!!.createMargin().newOrder(parameters)
                                Log.d("result", result)
                                OrderUtils.getSellOrderId(result)
                            } catch (_: Exception) {

                            }


//                            const += " #成交价:${OrderUtils.getOrderPrice(result)}# ，相差${TimeUtils.getTimeDiff(content)}秒"
                        }
                    }

                    content.contains("买单限价") -> {
                        var buyPrice = content.substringAfter("买单限价@")
                        buyPrice = String.format("%.8f", buyPrice.toFloat())
                        if (quantity.toFloat() < 1 && buyPrice.toFloat() > 0) {
                            if (MyApp.buyOrderId != 0L) {
                                val params: MutableMap<String, Any> = LinkedHashMap()
                                params.apply {
                                    put("symbol", "PEPEUSDT")
                                    put("orderId", MyApp.buyOrderId.toString())
                                }
                                try {
                                    MyApp.binanceApi!!.createMargin().cancelOpenOrders(params)
                                    MyApp.buyOrderId = 0L
                                } catch (_: Exception) {

                                }
                            }

                            var qq = OrderUtils.getMaxQuantity("PEPEUSDT")
                            if (MyApp.canBuyNum != 0L) {
                                qq = MyApp.canBuyNum
                            } else {
                                MyApp.canBuyNum = qq
                            }

                            parameters.apply {
                                put("symbol", "PEPEUSDT")
                                put("side", "BUY")
                                put("type", "LIMIT")
                                put("timeInForce", "GTC")
                                put("price", buyPrice)
                                put("quantity", qq.toString())
                            }
                            const += " 发起买单，价格:$buyPrice"
                            try {
                                val result = MyApp.binanceApi!!.createMargin().newOrder(parameters)
                                OrderUtils.getBuyOrderId(result)
                                Log.d("result", result)
                            } catch (_: Exception) {

                            }
                        } else {
                            const += " 当前还有余额，不执行"
                            Log.e("NotificationListenerService", "当前还有余额，不执行")
                        }
                    }
                }
*/
                list.add(const)
                SPUtils.put(this@NotificationListenerService, "orderList", gson.toJson(list))
            }
        }

    }

}
