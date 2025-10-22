import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.socket.client.IO
import io.socket.client.Socket
import tn.esprit.testsocket.ui.theme.Purple80
import tn.esprit.testsocket.ui.theme.PurpleGrey40
import tn.esprit.testsocket.ui.theme.TestSocketTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    var messages by remember { mutableStateOf(listOf<String>()) }
    var welcomeMessage by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }  // State to track socket connection

    // Create socket reference
    val options = IO.Options.builder().setTransports(arrayOf("websocket")).build()
    val socket = remember {
        IO.socket("http://192.168.100.22:3000", options)
    }

    // Clean up socket when page is deleted
    DisposableEffect(Unit) {
        onDispose {
            socket.disconnect()
            socket.off()
        }
    }

    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

        // User name input field
        OutlinedTextField(
            value = userName,
            onValueChange = { text -> userName = text },
            label = { Text("Username") },
            readOnly = isConnected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Button to connect to socket
        Button(
            onClick = {
                if (userName.isNotEmpty() && !socket.connected()) {
                    socket.connect()  // Establish socket connection

                    socket.on(Socket.EVENT_CONNECT) {
                        // Emit join event with username when connected
                        socket.emit("join", userName)
                        isConnected = true  // Update connection status
                    }

                    socket.on("userJoined") { args ->
                        if (args.isNotEmpty()) {
                            val message = args[0].toString()
                            welcomeMessage = message
                        }
                    }

                    socket.on("chat") { args ->
                        if (args.isNotEmpty()) {
                            Log.d("teb3ath", args[0].toString())
                            val message = args[0].toString()
                            messages = messages.plus(message)
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Purple80),
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = if (isConnected) "Connected" else "Connect to Chat")
        }

        // Show messages when user is connected
        if (isConnected) {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
//                reverseLayout = true,
                verticalArrangement = Arrangement.Top
            ) {
                item {
                    Text(welcomeMessage)
                }
                items(messages.reversed()) { message ->
                    ChatBubble(message, userName)
                }
            }
            // Input field for sending messages
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { text -> input = text },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
                IconButton(onClick = {
                    // Send message to server
                    socket.emit("chat", input)
                    input = ""  // Clear the input field after sending
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, "")
                }
            }
        } else {
            Text("Please connect first to join the chat", color = Color.Gray, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun ChatBubble(message: String, userName: String) {
    val bubbleColor =
        if (message.contains(userName)) PurpleGrey40 else Purple80

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = if (message.contains(userName)) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
                .widthIn(max = 200.dp)
        ) {
            Text(text = message, fontSize = 16.sp, color = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    TestSocketTheme {
        ChatScreen()
    }
}
