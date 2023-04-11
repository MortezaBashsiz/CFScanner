package ir.filternet.cfscanner.scanner

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory


class TLSSocketFactory : SSLSocketFactory() {
    private lateinit var delegate: SSLSocketFactory

    init {
        try {
            val context = SSLContext.getInstance("TLSv1.2")
            context.init(null, null, null)
            delegate = context.socketFactory
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val sslSocket = delegate.createSocket(socket, host, port, autoClose) as SSLSocket
        sslSocket.enabledProtocols = arrayOf("TLSv1.2")
        return sslSocket
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return delegate.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return delegate.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        return delegate.createSocket(host, port)
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress?, port: Int): Socket {
        return delegate.createSocket(host, port)
    }

    @Throws(IOException::class)
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
        return delegate.createSocket(host, port, localHost, localPort)
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
        return delegate.createSocket(address, port, localAddress, localPort)
    }
}