package com.monster.literaryflow.service

import fi.iki.elonen.NanoHTTPD
import java.io.IOException


class MyNanoHttpdServer(hostname: String?, port: Int) : NanoHTTPD(hostname, port) {
    override fun serve(session: IHTTPSession): Response {
        //TODO  解决客户端请求参数携带中文，出现中文乱码问题
        val ct = ContentType(session.headers["content-type"]).tryUTF8()
        session.headers["content-type"] = ct.contentTypeHeader
        return dealWith(session)
    }

    private fun dealWith(session: IHTTPSession): Response {
        if (Method.POST == session.method) {
            //获取请求头数据
            val header = session.headers
            //获取传参参数
            val params: Map<String, String> = HashMap()
            try {
                session.parseBody(params)
                var paramStr = params["postData"]
                if (paramStr.isNullOrEmpty()) {
                    return newFixedLengthResponse("success")
                }
                paramStr = paramStr!!.replace("\r\n", " ")
//                val jsonParam: JSONObject = JSON.parseObject(paramStr)
//                val result: Map<String, Any> = HashMap()
                //TODO 写你的业务逻辑.....
                //响应客户端
                return newFixedLengthResponse("success")
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ResponseException) {
                e.printStackTrace()
            }
            return newFixedLengthResponse("success")
        } else if (Method.GET == session.method) {
            val parameters = session.parameters
            return newFixedLengthResponse("success")
        }
        return newFixedLengthResponse("404")
    }

    companion object {
        //声明服务端 端口
        private const val HTTP_PORT = 49152

        @Volatile
        private var myNanoHttpdServer: MyNanoHttpdServer? = null

        //TODO	单例模式，获取实例对象，并传入当前机器IP
        fun getInstance(ipAddress: String?): MyNanoHttpdServer? {
            if (myNanoHttpdServer == null) {
                synchronized(MyNanoHttpdServer::class.java) {
                    if (myNanoHttpdServer == null) {
                        myNanoHttpdServer =
                            MyNanoHttpdServer(ipAddress, HTTP_PORT)
                    }
                }
            }
            return myNanoHttpdServer
        }

        fun newFixedLengthResponse(msg: String?): Response {
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, msg)
        }
    }
}

