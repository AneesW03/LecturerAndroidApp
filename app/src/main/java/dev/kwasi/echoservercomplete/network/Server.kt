package dev.kwasi.echoservercomplete.network

import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.Exception
import kotlin.concurrent.thread
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.Charsets.UTF_8

/// The [Server] class has all the functionality that is responsible for the 'server' connection.
/// This is implemented using TCP. This Server class is intended to be run on the GO.

class Server(private val iFaceImpl:NetworkMessageInterface) {
    companion object {
        const val PORT: Int = 9999

    }

    private val svrSocket: ServerSocket = ServerSocket(PORT, 0, InetAddress.getByName("192.168.49.1"))
    private val clientMap: HashMap<String, Socket> = HashMap()

    init {
        thread{
            while(true){
                try{
                    val clientConnectionSocket = svrSocket.accept()
                    Log.e("SERVER", "The server has accepted a connection: ")
                    handleSocket(clientConnectionSocket)

                }catch (e: Exception){
                    Log.e("SERVER", "An error has occurred in the server!")
                    e.printStackTrace()
                }
            }
        }
    }


    private fun handleSocket(socket: Socket){
        socket.inetAddress.hostAddress?.let {
            clientMap[it] = socket
            Log.e("SERVER", "A new connection has been detected!")
            thread {
                val clientReader = socket.inputStream.bufferedReader()
                val clientWriter = socket.outputStream.bufferedWriter()
                var receivedJson: String?

                val initialMessage = clientReader.readLine()
                if (initialMessage != null) {
                    val clientContent = Gson().fromJson(initialMessage, ContentModel::class.java)
                    if (clientContent.message == "I am here") {

                        // 2. Generate a random number R as the challenge
                        val challenge = (100000..999999).random().toString()

                        // 3. Send the challenge (R) to the client
                        clientWriter.write(Gson().toJson(ContentModel(challenge, "192.168.49.1")) + "\n")
                        clientWriter.flush()

                        // 4. Wait for the encrypted challenge response from the client
                        val encryptedResponse = clientReader.readLine()
                        if (encryptedResponse != null) {
                            val clientResponse =
                                Gson().fromJson(encryptedResponse, ContentModel::class.java)

                            // Hash the StudentID (assumed known)
                            val studentID =
                                "816030569" // The actual student ID should be checked
                            val hashedStudentID = hashStrSha256(studentID)

                            // 5. Generate the AES key and IV
                            val aesKey = generateAESKey(hashedStudentID)
                            val aesIv = generateIV(hashedStudentID)

                            // 6. Decrypt the client's response
                            val decryptedResponse =
                                decryptMessage(clientResponse.message, aesKey, aesIv)

                            // 7. Verify if the decrypted response matches the original challenge
                            if (decryptedResponse == challenge) {
                                Log.e("SERVER", "Authenticated")
                            }
                            else {
                                Log.e("SERVER", "Authentication failed")
                            }
                        }
                    }
                }

                while(socket.isConnected){
                    try{
                        receivedJson = clientReader.readLine()
                        if (receivedJson!= null){
                            Log.e("SERVER", "Received a message from client $it")
                            val clientContent = Gson().fromJson(receivedJson, ContentModel::class.java)
                            val reversedContent = ContentModel(clientContent.message.reversed(), "192.168.49.1")

                            val reversedContentStr = Gson().toJson(reversedContent)
                            clientWriter.write("$reversedContentStr\n")
                            clientWriter.flush()

                            // To show the correct alignment of the items (on the server), I'd swap the IP that it came from the client
                            // This is some OP hax that gets the job done but is not the best way of getting it done.
                            val tmpIp = clientContent.senderIp
                            clientContent.senderIp = reversedContent.senderIp
                            reversedContent.senderIp = tmpIp

                            iFaceImpl.onContent(clientContent)
                            iFaceImpl.onContent(reversedContent)

                        }
                    } catch (e: Exception){
                        Log.e("SERVER", "An error has occurred with the client $it")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun close(){
        svrSocket.close()
        clientMap.clear()
    }

}

    fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

    fun hashStrSha256(str: String): String{
        val algorithm = "SHA-256"
        val hashedString = MessageDigest.getInstance(algorithm).digest(str.toByteArray(UTF_8))
        return hashedString.toHex();
    }

    fun getFirstNChars(str: String, n:Int) = str.substring(0,n)

    fun generateAESKey(seed: String): SecretKeySpec {
        val first32Chars = getFirstNChars(seed,32)
        val secretKey = SecretKeySpec(first32Chars.toByteArray(), "AES")
        return secretKey
    }

    fun generateIV(seed: String): IvParameterSpec {
        val first16Chars = getFirstNChars(seed, 16)
        return IvParameterSpec(first16Chars.toByteArray())
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decryptMessage(encryptedText: String, aesKey: SecretKey, aesIv: IvParameterSpec):String{
        val textToDecrypt = Base64.Default.decode(encryptedText)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

        cipher.init(Cipher.DECRYPT_MODE, aesKey,aesIv)

        val decrypt = cipher.doFinal(textToDecrypt)
        return String(decrypt)
    }
