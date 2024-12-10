package com.monster.literaryflow.utils

import android.util.Log
import com.monster.literaryflow.MyApp
import io.gate.gateapi.models.FuturesOrder
import org.json.JSONArray
import org.json.JSONObject


object OrderUtils {
    fun createBuy(name: String, size: Long) {
        val futuresOrder = FuturesOrder()
        futuresOrder.contract = name
        futuresOrder.size = size
        futuresOrder.iceberg = 0
        futuresOrder.price = "0"
        futuresOrder.tif = FuturesOrder.TifEnum.IOC
        futuresOrder.text = "t-APP"
        MyApp.futuresApi?.createFuturesOrder("usdt", futuresOrder)
    }

    fun createSell(name: String,size:Long = 0L) {
        if (MyApp.amount==0){
            val balance = MyApp.walletApi!!.totalBalance
            balance.execute().details
        }
        val futuresOrder = FuturesOrder()
        futuresOrder.contract = name
        futuresOrder.size = 0
        futuresOrder.close = true
        futuresOrder.iceberg = 0
        futuresOrder.price = "0"
        futuresOrder.tif = FuturesOrder.TifEnum.IOC
        futuresOrder.text = "t-APP"
        MyApp.futuresApi?.createFuturesOrder("usdt", futuresOrder)
    }

    fun getOrderPrice(orderStr:String):String{
        val prices = mutableListOf<String>()
        val jsonObject = JSONObject(orderStr)
        val fillsArray = jsonObject.getJSONArray("fills")
        val fillObject = fillsArray.getJSONObject(0)
        return fillObject.getString("price")
    }
    fun getBuyOrderId(orderStr:String):Long{
        val prices = mutableListOf<String>()
        val jsonObject = JSONObject(orderStr)
        val orderId = jsonObject.getLong("orderId")
        MyApp.buyOrderId = orderId
        return orderId
    }
    fun getSellOrderId(orderStr:String):Long{
        val prices = mutableListOf<String>()
        val jsonObject = JSONObject(orderStr)
        val orderId = jsonObject.getLong("orderId")
        MyApp.sellOrderId = orderId
        return orderId
    }

/*
    fun getMarginBalance(name:String):String{
        val parameters: MutableMap<String, Any> = LinkedHashMap()
        val order = MyApp.binanceApi!!.createMargin().account(parameters)
        val jsonObject = JSONObject(order)
        var quantity = "0"
        val userAssets = jsonObject.getJSONArray("userAssets")
        for (i in 0 until userAssets.length()) {
            val assetObject = userAssets.getJSONObject(i)
            val assetName = assetObject.getString("asset")
            if (assetName == name) {
                quantity=assetObject.getString("netAsset")
            }
        }
        return quantity
    }
*/

/*    fun getMaxQuantity(name:String):Long{
        val parameters: MutableMap<String, Any> = LinkedHashMap()
        parameters.apply {
            put("symbol", name)
        }
        val priceStr = MyApp.binanceApi!!.createMarket().averagePrice(parameters)
        Log.d("getMaxQuantity","PEPE当前价格 : $priceStr")
        val jsonObject = JSONObject(priceStr)
        val price = jsonObject.getString("price").toFloat()
        val balance = getMarginBalance("USDT")
        Log.d("getMaxQuantity"," USDT余额 : $balance")
        val quantity = (balance.toFloat()*4.8/price.toFloat()).toLong()
        Log.d("getMaxQuantity"," 最大交易量$quantity")
        return quantity
    }*/
/*
    fun getMoreU(){
        val parameters: MutableMap<String, Any> = LinkedHashMap()
        parameters.apply {
            put("asset", "PEPE")
        }
        val max = MyApp.binanceApi!!.createMargin().maxBorrow(parameters)
        val jsonObject = JSONObject(max)
        val amount = jsonObject.getString("amount").toFloat().toInt()
        Log.d("getMaxQuantity"," 最大可借$amount")
        val parameters1: MutableMap<String, Any> = LinkedHashMap()
        parameters1.apply {
            put("asset", "PEPE")
            put("amount", amount.toString())
        }
        MyApp.binanceApi!!.createMargin().borrow(parameters1)
        getMaxQuantity("PEPEUSDT")
    }
*/
/*
    fun repayMoreU(){
        val parameters: MutableMap<String, Any> = LinkedHashMap()
        parameters.apply {
            put("asset", "USDT")
        }
        MyApp.binanceApi!!.createMargin().repay(parameters)
    }
*/
    fun createOrder(
        symbol: String,
        side: String,
        price: String,
        quantity: Float,
        stopPrice: String
    ) {
    Log.d("checkAndUpdateOrders","$price ,$stopPrice")

    val parameters = LinkedHashMap<String, Any>()
        parameters["newClientOrderId"] = "BUY"
        parameters["symbol"] = symbol
        parameters["side"] = side
        parameters["type"] = "LIMIT"
        parameters["price"] =price
        parameters["timeInForce"] =  "GTC"
        parameters["priceProtect"] = "true"
        parameters["quantity"] =quantity
        MyApp.binanceFuturesApi!!.account().newOrder(parameters)
    }
    fun createWinOrder(
        symbol: String,
        quantity: Float,
        stopPrice: String
    ) {
        val openOrders = getOpenOrders()
        Log.d("checkAndUpdateOrders","$openOrders")
        if (openOrders.isNotEmpty()) {
            val parameters = LinkedHashMap<String, Any>()
            parameters["newClientOrderId"] = "BUY_WIN"
            parameters["symbol"] = symbol
            parameters["side"] = "SELL"
            parameters["type"] = "TAKE_PROFIT"
            parameters["price"] =stopPrice
            parameters["stopPrice"] =stopPrice
            parameters["timeInForce"] =  "GTC"
            parameters["priceProtect"] = "true"
            parameters["quantity"] =quantity
            parameters["reduceOnly"] ="true"
            MyApp.binanceFuturesApi!!.account().newOrder(parameters)
        }

    }

    // Function to cancel all open orders
    fun cancelAllOrders(symbol: String) {
        val params = mapOf("symbol" to symbol)
//        MyApp.binanceApi!!.createTrade().cancelOpenOrders(params)
    }

    // Function to get all open orders
    fun getOpenOrders(): List<JSONObject> {
        val parameters = LinkedHashMap<String, Any>()
        parameters["symbol"] = "1000PEPEUSDT";
        val response =MyApp.binanceFuturesApi!!.account().positionInformation(parameters)
        val jsonArray = JSONArray(response)
        return List(jsonArray.length()) { index -> jsonArray.getJSONObject(index) }
    }

    // Function to check and update orders
    fun checkAndUpdateOrders(buyLimitPrice: Float, takeProfitPrice: Float) {
        val openOrders = getOpenOrders()
        Log.d("checkAndUpdateOrders","$openOrders")
        if (openOrders.isNotEmpty()) {
            cancelAllOrders("1000PEPEUSDT")
        }
        val buy = String.format("%.7f", buyLimitPrice)
        val sell = String.format("%.7f", takeProfitPrice)
        val parameter = LinkedHashMap<String, Any>()
        parameter["symbol"] = "1000PEPEUSDT"
        MyApp.binanceFuturesApi!!.account().cancelAllOpenOrders(parameter)
        if (buyLimitPrice != 0.0f) {
            createOrder("1000PEPEUSDT", "BUY", buy, 5000f, sell)
        }
        createWinOrder("1000PEPEUSDT",5000f,sell)
    }



}