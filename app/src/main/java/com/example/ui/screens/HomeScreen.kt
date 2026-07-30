package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.update.UpdateState

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import com.example.R
import java.text.SimpleDateFormat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AntigravityHeaderBadge
import com.example.data.AntigravityIntegrationCard
import com.example.data.ChatEntity
import com.example.data.UserEntity
import com.example.data.CallEntity
import com.example.data.StatusEntity
import com.example.data.CardEntity
import com.example.data.TransactionEntity
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.viewmodel.ChatViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ChatViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val chats by viewModel.chats.collectAsState()

    // Bottom Navigation Tab state
    var selectedTab by remember { mutableStateOf("chats") }

    // Calculate total unread chats count
    val totalUnreadCount = remember(chats) {
        chats.sumOf { it.unreadCount }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEAF2FB), // AmBle bg start (light-blue gradient)
                        Color(0xFFD6E8FA)  // AmBle bg end
                    )
                )
            )
    ) {
        val isWideScreen = maxWidth > 720.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isWideScreen) Modifier
                        .widthIn(max = 840.dp)
                        .align(Alignment.Center)
                    else Modifier
                )
        ) {
            // Tab contents switcher
            when (selectedTab) {
                "chats" -> {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                        ChatsTabContent(viewModel = viewModel, onChatSelect = { chatId ->
                            viewModel.selectChat(chatId)
                        })
                    }
                }
                "calls" -> {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                        CallsTabContent(viewModel = viewModel)
                    }
                }
                "status" -> {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                        StatusTabContent(viewModel = viewModel)
                    }
                }
                "wallet" -> {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                        WalletTabContent(viewModel = viewModel)
                    }
                }
            }

            // Floating Custom Bottom Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    unreadCount = totalUnreadCount,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 680.dp)
                )
            }
        }

    }
}

@Composable
fun BottomNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    unreadCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("floating_bottom_bar"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.70f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.80f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("chats", Icons.Default.Chat, "Chats"),
                Triple("calls", Icons.Default.Call, "Calls"),
                Triple("status", Icons.Default.CircleNotifications, "Updates"),
                Triple("wallet", Icons.Default.AccountBalanceWallet, "Wallet")
            )
            tabs.forEach { (tabId, icon, label) ->
                val isSelected = selectedTab == tabId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) AmBlePrimary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onTabSelected(tabId) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) AmBlePrimary else AmBleInkFaint,
                            modifier = Modifier.size(22.dp)
                        )
                        if (tabId == "chats" && unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(AmBlePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AmBlePrimary else AmBleInkFaint
                    )
                }
            }
        }
    }
}

@Composable
fun ChatsTabContent(
    viewModel: ChatViewModel,
    onChatSelect: (String) -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.users.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val supabaseStatus by viewModel.supabaseStatus.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showSupabaseDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var isListeningVoiceSearch by remember { mutableStateOf(false) }
    var showVoiceSearchDialog by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoiceSearch = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val query = spokenResults?.firstOrNull()
            if (!query.isNullOrBlank()) {
                searchQuery = query
            }
        }
    }

    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isEmpty()) chats
        else chats.filter { it.groupName.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true) }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isEmpty()) contacts
        else contacts.filter { it.contactName.contains(searchQuery, ignoreCase = true) || it.contactPhoneNumber.contains(searchQuery) }
    }

    if (showSupabaseDialog && currentUser?.isAdmin == true) {
        SupabaseIntegrationDialog(
            viewModel = viewModel,
            onDismiss = { showSupabaseDialog = false }
        )
    }

    if (showProfileDialog) {
        UserProfileDialog(
            viewModel = viewModel,
            onOpenSupabase = {
                showProfileDialog = false
                showSupabaseDialog = true
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showVoiceSearchDialog) {
        VoiceSearchDialog(
            onQuerySelected = { query ->
                searchQuery = query
                showVoiceSearchDialog = false
            },
            onDismiss = { showVoiceSearchDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // In-App Update Notice Banner / Overlay
        InAppUpdateBanner(viewModel = viewModel)

        // Custom Top Bar (AmBle style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showProfileDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = AmBleInk
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "AmBle App Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "AmBle Chats",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmBleInk
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gemini Live Voice Button
                Surface(
                    onClick = { viewModel.navigateTo("gemini_live_voice") },
                    shape = RoundedCornerShape(20.dp),
                    color = AmBlePrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AmBleSky.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Gemini Live Voice",
                            tint = AmBlePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Live Voice",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmBleInk
                        )
                    }
                }

                if (currentUser?.isAdmin == true) {
                    // Supabase Badge
                    Surface(
                        onClick = { showSupabaseDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF3ECF8E).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF3ECF8E).copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (supabaseStatus.isConnected) Color(0xFF3ECF8E) else Color(0xFFFFB020))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Supabase",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B2A5E)
                            )
                        }
                    }
                }

                // User Profile Image with Online Status Dot
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
                            .clickable { showProfileDialog = true }
                    ) {
                        AsyncImage(
                            model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(if (currentUser?.isOnline == true) Color(0xFF3ECF8E) else Color.Gray)
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                viewModel.setUserOnlineStatus(!(currentUser?.isOnline ?: true))
                            }
                    )
                }
            }
        }

        // Search Bar (Rounded pill-shaped with Voice-to-Text)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search chats...", color = Color(0xFF8BA3C7), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF8BA3C7)) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFF8BA3C7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            try {
                                isListeningVoiceSearch = true
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to filter chats...")
                                }
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                isListeningVoiceSearch = false
                                showVoiceSearchDialog = true
                            }
                        },
                        modifier = Modifier.testTag("voice_search_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = if (isListeningVoiceSearch) Color(0xFFEF4444) else Color(0xFF2B52E1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        // Persistent Quick Contacts Carousel Integration
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contacts Directory (${contacts.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
            Text(
                text = "+ Add Contact",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB),
                modifier = Modifier
                    .clickable { showNewChatDialog = true }
                    .testTag("add_contact_quick_button")
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showNewChatDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "New Contact",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("New", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                }
            }

            items(contacts) { contact ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.openChatByPhoneNumber(contact.contactPhoneNumber) }
                        .testTag("quick_contact_${contact.contactPhoneNumber}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.contactName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.contactName.split(" ").firstOrNull() ?: contact.contactName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Gemini AI Capabilities Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini AI Engine Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "⚡ Low-Latency Mode",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Voice Conversations: gemini-3.1-flash-live-preview (Live API)\n• Ultra Fast Responses: gemini-3.1-flash-lite",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.navigateTo("gemini_live_voice") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Gemini Live Voice Conversation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chats list Header with "New Chat" button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Conversations",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A5E)
            )
            IconButton(onClick = { showNewChatDialog = true }) {
                Icon(
                    imageVector = Icons.Default.AddComment,
                    contentDescription = "New Chat",
                    tint = Color(0xFF3B7DD8)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val isPhoneNumberQuery = remember(searchQuery) {
            val clean = searchQuery.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
            clean.isNotEmpty() && (clean.startsWith("+") || clean.all { it.isDigit() }) && clean.length >= 5
        }

        val filteredUsers = remember(users, searchQuery) {
            if (searchQuery.isEmpty()) emptyList()
            else users.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.phoneNumber.contains(searchQuery) 
            }
        }

        // Chats LazyColumn / Search Results
        if (searchQuery.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Direct Phone Search Offer
                if (isPhoneNumberQuery) {
                    item {
                        Text(
                            text = "Direct Entry",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B7DD8),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openChatByPhoneNumber(searchQuery) },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FB)),
                            border = BorderStroke(1.dp, Color(0xFF3B7DD8).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B7DD8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Direct Phone",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Direct Entry: $searchQuery",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1B2A5E)
                                    )
                                    Text(
                                        text = "Tap to chat & start HD Voice/Video calls directly",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Go",
                                    tint = Color(0xFF1B2A5E)
                                )
                            }
                        }
                    }
                }

                // Saved Contacts Search Section
                if (filteredContacts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Saved Contacts (${filteredContacts.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredContacts) { contact ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openChatByPhoneNumber(contact.contactPhoneNumber) }
                                .testTag("search_contact_card_${contact.contactPhoneNumber}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.contactName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.contactName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = contact.contactPhoneNumber,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Button(
                                    onClick = { viewModel.openChatByPhoneNumber(contact.contactPhoneNumber) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Recent Conversations Section
                if (filteredChats.isNotEmpty()) {
                    item {
                        Text(
                            text = "Matching Chats",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8BA3C7),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredChats) { chat ->
                        ChatListItem(
                            chat = chat,
                            users = users,
                            currentUserId = currentUser?.uid,
                            onClick = { onChatSelect(chat.chatId) }
                        )
                    }
                }

                // Other Users/Contacts Section
                val unmatchedUsers = filteredUsers.filter { u -> 
                    !filteredChats.any { c -> c.groupName.equals(u.name, ignoreCase = true) }
                }
                if (unmatchedUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Global Directory / Contacts",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8BA3C7),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(unmatchedUsers) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openOneOnOneChatWithUser(user) },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEAF2FB))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1B2A5E)
                                    )
                                    if (user.isTyping) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AnimatedTypingDots(color = Color(0xFF2563EB), dotSize = 5.dp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "typing…",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "${user.phoneNumber} • ${user.status}",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Open Chat",
                                    tint = Color(0xFF3B7DD8)
                                )
                            }
                        }
                    }
                }

                if (!isPhoneNumberQuery && filteredChats.isEmpty() && unmatchedUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No results found for \"$searchQuery\"",
                                color = Color(0xFF8BA3C7),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Normal Chats list
            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No chats found",
                        color = Color(0xFF8BA3C7),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredChats) { chat ->
                        ChatListItem(
                            chat = chat,
                            users = users,
                            currentUserId = currentUser?.uid,
                            onClick = { onChatSelect(chat.chatId) }
                        )
                    }
                }
            }
        }
    }

    // New Chat dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = {
                Text(
                    text = "Start a Chat",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B2A5E)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select a contact to start chatting:", fontSize = 13.sp, color = Color.Gray)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(users) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.openOneOnOneChatWithUser(user)
                                        showNewChatDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B2A5E)
                                    )
                                    Text(
                                        text = user.status,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun ChatListItem(
    chat: ChatEntity,
    users: List<UserEntity> = emptyList(),
    currentUserId: String? = null,
    onClick: () -> Unit
) {
    val typingUser = remember(chat, users, currentUserId) {
        if (chat.isGroup) {
            users.firstOrNull { it.uid != currentUserId && it.isTyping }
        } else {
            val parts = chat.chatId.split("_")
            val otherUserId = parts.firstOrNull { it != currentUserId }
            users.firstOrNull { 
                (otherUserId != null && it.uid == otherUserId) || it.name.equals(chat.groupName, ignoreCase = true) 
            }?.takeIf { it.isTyping }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2FB))
            ) {
                AsyncImage(
                    model = chat.groupPhoto,
                    contentDescription = chat.groupName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text and info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.groupName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1B2A5E)
                    )
                    
                    val timeString = remember(chat.lastMessageTime) {
                        try {
                            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                            sdf.format(Date(chat.lastMessageTime))
                        } catch (e: Exception) {
                            "12:00 PM"
                        }
                    }
                    Text(
                        text = timeString,
                        fontSize = 11.sp,
                        color = Color(0xFF8BA3C7)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (typingUser != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            AnimatedTypingDots(color = Color(0xFF2563EB), dotSize = 5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (chat.isGroup) "${typingUser.name} is typing…" else "typing…",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = chat.lastMessage,
                            fontSize = 13.sp,
                            color = Color(0xFF8BA3C7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B7DD8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallsTabContent(
    viewModel: ChatViewModel
) {
    val calls by viewModel.calls.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var selectedServerTab by remember { mutableStateOf("Signaling") } // "Signaling", "Coturn", "Traversal"
    var isSyncingContacts by remember { mutableStateOf(false) }
    var syncSuccessMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Custom Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color(0xFF1B2A5E)
                    )
                }

                Text(
                    text = "Calls & Infrastructure",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B2A5E)
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                ) {
                    AsyncImage(
                        model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 2. REAL APP CONTACT SYNC & FRIEND LOCATOR
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = Color(0xFF1B2A5E),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Contact Sync & Active App Users",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B2A5E)
                            )
                        }

                        Button(
                            onClick = {
                                isSyncingContacts = true
                                scope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    isSyncingContacts = false
                                    syncSuccessMsg = "Synced ${users.size} active contacts using AmBle!"
                                }
                            },
                            enabled = !isSyncingContacts,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSyncingContacts) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("Sync Contacts", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    if (syncSuccessMsg != null) {
                        Text(
                            text = syncSuccessMsg!!,
                            color = Color(0xFF25D366),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "When your friends register on AmBle with their phone numbers, they automatically appear here for instant Voice & Video calling:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Registered users list for immediate calls
                    val registeredFriends = users.filter { it.uid != currentUser?.uid }
                    if (registeredFriends.isEmpty()) {
                        Text(text = "No other users synced yet.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        registeredFriends.forEach { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F7FC))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0E7FF))
                                    ) {
                                        AsyncImage(
                                            model = friend.photoUrl,
                                            contentDescription = friend.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = friend.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1B2A5E)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E676))
                                            )
                                        }
                                        Text(
                                            text = friend.phoneNumber.ifEmpty { "AmBle User" },
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Voice call button
                                    IconButton(
                                        onClick = {
                                            viewModel.startCall(friend.uid, "audio")
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF25D366).copy(alpha = 0.15f))
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                                    }

                                    // Video call button
                                    IconButton(
                                        onClick = {
                                            viewModel.startCall(friend.uid, "video")
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B7DD8).copy(alpha = 0.15f))
                                    ) {
                                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color(0xFF3B7DD8), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. RECENT CALL HISTORY
        item {
            Text(
                text = "Recent Call Log",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A5E)
            )
        }

        if (calls.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent calls logged yet.",
                        color = Color(0xFF8BA3C7),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(calls) { call ->
                val otherParticipant = remember(call, users, currentUser) {
                    val partnerId = if (call.callerId == currentUser?.uid) call.calleeId else call.callerId
                    users.firstOrNull { it.uid == partnerId }
                }
                CallHistoryItem(call = call, otherUser = otherParticipant, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CallHistoryItem(call: CallEntity, otherUser: UserEntity?, viewModel: ChatViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (otherUser != null) {
                    viewModel.openOneOnOneChatWithUser(otherUser)
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2FB))
            ) {
                AsyncImage(
                    model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    contentDescription = otherUser?.name ?: "User",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = otherUser?.name ?: "Unknown User",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1B2A5E)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (call.status == "missed" || call.status == "declined") Icons.Default.CallMissed
                        else if (call.callerId == otherUser?.uid) Icons.Default.CallReceived
                        else Icons.Default.CallMade,
                        contentDescription = null,
                        tint = if (call.status == "missed" || call.status == "declined") Color(0xFFF76C6C) else Color(0xFF2ECC71),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    val dateString = remember(call.startedAt) {
                        try {
                            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            sdf.format(Date(call.startedAt))
                        } catch (e: Exception) {
                            "Recent"
                        }
                    }
                    Text(
                        text = "$dateString • ${call.type.replaceFirstChar { it.uppercase() }}",
                        fontSize = 12.sp,
                        color = Color(0xFF8BA3C7)
                    )
                }
            }

            // Quick Call button
            IconButton(
                onClick = {
                    if (otherUser != null) {
                        viewModel.openOneOnOneChatWithUser(otherUser)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2FB))
            ) {
                Icon(
                    imageVector = if (call.type == "video") Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Call again",
                    tint = Color(0xFF3B7DD8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatusTabContent(
    viewModel: ChatViewModel
) {
    val statuses by viewModel.statuses.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Custom Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color(0xFF1B2A5E)
                )
            }

            Text(
                text = "Updates",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A5E)
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            ) {
                AsyncImage(
                    model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // My Status card
        Text(
            text = "My Status",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B2A5E)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF3B7DD8), CircleShape)
                        .background(Color(0xFFEAF2FB))
                ) {
                    AsyncImage(
                        model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                        contentDescription = "My avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1B2A5E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to add a new status update",
                        fontSize = 12.sp,
                        color = Color(0xFF8BA3C7)
                    )
                }

                IconButton(
                    onClick = { /* Add Status */ },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B7DD8))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add status",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recent Updates",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B2A5E)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (statuses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No status updates",
                    color = Color(0xFF8BA3C7),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(statuses) { status ->
                    StatusRowItemCustom(status = status)
                }
            }
        }
    }
}

@Composable
fun StatusRowItemCustom(status: StatusEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, Color(0xFF2ECC71), CircleShape) // Green indicator for unviewed status
                    .background(Color(0xFFEAF2FB))
            ) {
                AsyncImage(
                    model = status.userPhoto,
                    contentDescription = status.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1B2A5E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.text,
                    fontSize = 12.sp,
                    color = Color(0xFF8BA3C7),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val dateString = remember(status.timestamp) {
                try {
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    sdf.format(Date(status.timestamp))
                } catch (e: Exception) {
                    "Just now"
                }
            }
            Text(
                text = dateString,
                fontSize = 11.sp,
                color = Color(0xFF8BA3C7)
            )
        }
    }
}

@Composable
fun WalletTabContent(viewModel: ChatViewModel) {
    val cards by viewModel.cards.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showSendMoneyDialog by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }

    val totalBalance = remember(cards) {
        cards.sumOf { it.balance }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        // Custom Top Bar for Wallet
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Wallet Icon",
                    tint = Color(0xFF1B2A5E),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Text(
                text = "My Wallet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A5E)
            )

            IconButton(
                onClick = { showAddCardDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Card",
                    tint = Color(0xFF1B2A5E)
                )
            }
        }

        // Total Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("wallet_balance_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A5E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Active Balance",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format(Locale.US, "$%,.2f", totalBalance),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Cards",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = cards.size.toString(),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Transactions",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = transactions.size.toString(),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cards Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Cards",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A5E)
            )
            if (cards.isNotEmpty()) {
                Text(
                    text = "Swipe to view • Long-press to delete",
                    fontSize = 11.sp,
                    color = Color(0xFF8BA3C7)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cards Carousel
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .border(1.dp, Color(0xFFD6E8FA), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "No cards",
                        tint = Color(0xFF8BA3C7),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No cards added yet",
                        color = Color(0xFF1B2A5E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(onClick = { showAddCardDialog = true }) {
                        Text("Add a card now", color = Color(0xFF3B7DD8))
                    }
                }
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cards_carousel"),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(cards) { card ->
                    CardItemView(
                        card = card,
                        onDeleteCard = { viewModel.deleteCard(card.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Row
        Text(
            text = "Quick Actions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B2A5E)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val isEnabled = cards.isNotEmpty()
            QuickActionButton(
                label = "Send Money",
                icon = Icons.Default.ArrowOutward,
                backgroundColor = if (isEnabled) Color(0xFFFFECEF) else Color(0xFFE5EBF2),
                iconColor = if (isEnabled) Color(0xFFD83C3C) else Color(0xFF8BA3C7),
                onClick = { if (isEnabled) showSendMoneyDialog = true }
            )
            QuickActionButton(
                label = "Top Up",
                icon = Icons.Default.AddCircleOutline,
                backgroundColor = if (isEnabled) Color(0xFFEDFBF3) else Color(0xFFE5EBF2),
                iconColor = if (isEnabled) Color(0xFF229A51) else Color(0xFF8BA3C7),
                onClick = { if (isEnabled) showTopUpDialog = true }
            )
            QuickActionButton(
                label = "Add Card",
                icon = Icons.Default.AddCard,
                backgroundColor = Color(0xFFEEF5FF),
                iconColor = Color(0xFF3B7DD8),
                onClick = { showAddCardDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Transactions Section Header
        Text(
            text = "Recent Transactions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B2A5E)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent transactions found",
                    color = Color(0xFF8BA3C7),
                    fontSize = 13.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                transactions.take(10).forEach { tx ->
                    TransactionItemView(tx = tx)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialogs
    if (showAddCardDialog) {
        AddCardDialog(
            onDismiss = { showAddCardDialog = false },
            onConfirm = { cardType, cardNo, holder, expiry, initialBal, colorHex ->
                viewModel.addCard(cardType, cardNo, holder, expiry, initialBal, colorHex)
                showAddCardDialog = false
            }
        )
    }

    if (showSendMoneyDialog) {
        SendMoneyDialog(
            cards = cards,
            onDismiss = { showSendMoneyDialog = false },
            onConfirm = { title, category, amount, cardId ->
                viewModel.addTransaction(title, category, -amount, cardId)
                showSendMoneyDialog = false
            }
        )
    }

    if (showTopUpDialog) {
        TopUpDialog(
            cards = cards,
            onDismiss = { showTopUpDialog = false },
            onConfirm = { title, amount, cardId ->
                viewModel.addTransaction(title, "Top Up", amount, cardId)
                showTopUpDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardItemView(
    card: CardEntity,
    onDeleteCard: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val bgColor = remember(card.cardColorHex) {
        try {
            Color(android.graphics.Color.parseColor(card.cardColorHex))
        } catch (e: Exception) {
            Color(0xFF1B2A5E)
        }
    }

    Card(
        modifier = Modifier
            .width(260.dp)
            .height(155.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showDeleteConfirm = true }
            )
            .testTag("card_item_${card.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            // Card Issuer / Chip decoration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.cardType,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                // Simulated microchip
                Box(
                    modifier = Modifier
                        .size(32.dp, 24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }

            // Card Balance
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = "Balance",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
                Text(
                    text = String.format(Locale.US, "$%,.2f", card.balance),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            // Card Number / Expiry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = card.cardHolder.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(150.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = card.cardNumber,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPIRY",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp
                    )
                    Text(
                        text = card.expiryDate,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Card?") },
            text = { Text("Are you sure you want to delete this ${card.cardType} card ending in ${card.cardNumber.takeLast(4)}?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCard()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionItemView(tx: TransactionEntity) {
    val categoryIcon = remember(tx.category) {
        when (tx.category.lowercase()) {
            "flight", "travel" -> Icons.Default.Flight
            "shopping" -> Icons.Default.ShoppingBag
            "salary" -> Icons.Default.AttachMoney
            "food" -> Icons.Default.Restaurant
            "entertainment" -> Icons.Default.LocalPlay
            "top up" -> Icons.Default.TrendingUp
            else -> Icons.Default.Payments
        }
    }

    val iconBgColor = remember(tx.category) {
        when (tx.category.lowercase()) {
            "flight", "travel" -> Color(0xFFEDF5FF)
            "shopping" -> Color(0xFFFFF7EC)
            "salary" -> Color(0xFFEDFBF3)
            "food" -> Color(0xFFFBECEF)
            "entertainment" -> Color(0xFFF3EDFF)
            "top up" -> Color(0xFFE6F9F0)
            else -> Color(0xFFF0F4F8)
        }
    }

    val iconColor = remember(tx.category) {
        when (tx.category.lowercase()) {
            "flight", "travel" -> Color(0xFF3B7DD8)
            "shopping" -> Color(0xFFFFA23A)
            "salary" -> Color(0xFF229A51)
            "food" -> Color(0xFFD83C3C)
            "entertainment" -> Color(0xFF8F3BD8)
            "top up" -> Color(0xFF2E7D32)
            else -> Color(0xFF5E7A8C)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = tx.category,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1B2A5E)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tx.dateText,
                fontSize = 11.sp,
                color = Color(0xFF8BA3C7)
            )
        }

        val isPositive = tx.amount > 0
        val sign = if (isPositive) "+" else ""
        Text(
            text = String.format(Locale.US, "%s$%,.2f", sign, tx.amount),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isPositive) Color(0xFF229A51) else Color(0xFFD83C3C)
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1B2A5E)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Double, String) -> Unit
) {
    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("") }
    var selectedCardType by remember { mutableStateOf("Visa") }
    var selectedCardColor by remember { mutableStateOf("#1A51A6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Card", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Card Issuer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf("Visa", "MasterCard").forEach { type ->
                        val selected = selectedCardType == type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCardType = type },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) Color(0xFFEEF5FF) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF3B7DD8) else Color(0xFFD6E8FA))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                Text(type, fontWeight = FontWeight.Bold, color = if (selected) Color(0xFF3B7DD8) else Color(0xFF8BA3C7))
                            }
                        }
                    }
                }

                Text("Card Design Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val colors = listOf(
                        "#1A51A6" to "Midnight",
                        "#FF5F00" to "Flame",
                        "#2E7D32" to "Forest",
                        "#C2185B" to "Velvet",
                        "#455A64" to "Charcoal"
                    )
                    colors.forEach { (hex, name) ->
                        val selected = selectedCardColor == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    border = BorderStroke(2.dp, if (selected) Color.White else Color.Transparent),
                                    shape = CircleShape
                                )
                                .clickable { selectedCardColor = hex }
                        )
                    }
                }

                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = { Text("Cardholder Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16) cardNumber = it.filter { char -> char.isDigit() } },
                    label = { Text("Card Number (16 digits)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry (MM/YY)") },
                    placeholder = { Text("e.g. 12/28") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Initial Balance ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBal = balanceStr.toDoubleOrNull() ?: 0.0
                    val formattedNum = if (cardNumber.length == 16) {
                        "${cardNumber.substring(0,4)} **** **** ${cardNumber.substring(12,16)}"
                    } else {
                        "4850 **** **** 9199"
                    }
                    val finalHolder = if (cardHolder.isBlank()) "Marcel L. Kissinger" else cardHolder
                    val finalExpiry = if (expiryDate.isBlank()) "12/28" else expiryDate
                    onConfirm(selectedCardType, formattedNum, finalHolder, finalExpiry, finalBal, selectedCardColor)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E))
            ) {
                Text("Add Card", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8BA3C7))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyDialog(
    cards: List<CardEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Shopping") }
    var selectedCardIndex by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Money / Pay", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("From Card", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cards.forEachIndexed { index, card ->
                        val selected = selectedCardIndex == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCardIndex = index },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) Color(0xFFEEF5FF) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF3B7DD8) else Color(0xFFD6E8FA))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(card.cardType + " (${card.cardNumber.takeLast(4)})", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                                    Text("Bal: " + String.format(Locale.US, "$%,.2f", card.balance), fontSize = 11.sp, color = Color(0xFF8BA3C7))
                                }
                                if (selected) {
                                    Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF3B7DD8), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                val categories = listOf("Shopping", "Flight", "Food", "Entertainment", "Other")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val finalTitle = if (title.isBlank()) "Quick Payment" else title
                    if (cards.isNotEmpty()) {
                        val card = cards[selectedCardIndex]
                        onConfirm(finalTitle, selectedCategory, amount, card.id)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD83C3C))
            ) {
                Text("Confirm Payment", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8BA3C7))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpDialog(
    cards: List<CardEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCardIndex by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Top Up Card / Deposit", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("To Card", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cards.forEachIndexed { index, card ->
                        val selected = selectedCardIndex == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCardIndex = index },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) Color(0xFFEEF5FF) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF3B7DD8) else Color(0xFFD6E8FA))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(card.cardType + " (${card.cardNumber.takeLast(4)})", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                                    Text("Bal: " + String.format(Locale.US, "$%,.2f", card.balance), fontSize = 11.sp, color = Color(0xFF8BA3C7))
                                }
                                if (selected) {
                                    Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF3B7DD8), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Source Description") },
                    placeholder = { Text("e.g. Bank Deposit, Salary") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val finalTitle = if (title.isBlank()) "Top Up Balance" else title
                    if (cards.isNotEmpty()) {
                        val card = cards[selectedCardIndex]
                        onConfirm(finalTitle, amount, card.id)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF229A51))
            ) {
                Text("Confirm Deposit", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8BA3C7))
            }
        }
    )
}

@Composable
fun SupabaseIntegrationDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val supabaseStatus by viewModel.supabaseStatus.collectAsState()
    val isSyncing by viewModel.isSupabaseSyncing.collectAsState()
    val syncResult by viewModel.supabaseSyncResult.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3ECF8E).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Supabase Cloud",
                        tint = Color(0xFF3ECF8E),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Supabase Cloud Integration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B2A5E)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (supabaseStatus.isConnected) Color(0xFFE8F9F0) else Color(0xFFFFF8E6)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (supabaseStatus.isConnected) Color(0xFF3ECF8E) else Color(0xFFFFB020)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (supabaseStatus.isConnected) Color(0xFF3ECF8E) else Color(0xFFFFB020))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (supabaseStatus.isConnected) "Connected to Supabase" else "Supabase Local & Cloud Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1B2A5E)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "URL: ${supabaseStatus.url}",
                            fontSize = 11.sp,
                            color = Color(0xFF556A8A)
                        )

                        if (supabaseStatus.errorMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: ${supabaseStatus.errorMessage}",
                                fontSize = 11.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                // Info banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5FB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚡ Real-time Cloud Synchronization",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFF1B2A5E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AmBle automatically connects with Supabase PostgreSQL database for real-time messaging, contact syncing, and end-to-end encrypted backup.",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7C96)
                        )
                    }
                }

                if (syncResult != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE3F2FD),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = syncResult!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0D47A1),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = { viewModel.syncDataWithSupabase() },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3ECF8E))
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Contacts & Messages Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.backupDataToSupabase() },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF1B2A5E))
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Backup", tint = Color(0xFF1B2A5E), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Encrypted Backup to Supabase", color = Color(0xFF1B2A5E), fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.checkSupabaseHealth() },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF8BA3C7))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Test", tint = Color(0xFF1B2A5E), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Server Connection", color = Color(0xFF1B2A5E))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E))
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
fun UserProfileDialog(
    viewModel: ChatViewModel,
    onOpenSupabase: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var isRememberLogin by remember { mutableStateOf(viewModel.isRememberLoginEnabled()) }
    var showConfirmSignOut by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header (Fixed at top)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account & Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B2A5E)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with online status badge
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color(0xFF3B7DD8), CircleShape)
                        ) {
                            AsyncImage(
                                model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                contentDescription = "Profile Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3ECF8E))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentUser?.name ?: "User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B2A5E)
                    )

                    val emailStr = currentUser?.email
                    if (!emailStr.isNullOrEmpty()) {
                        Text(
                            text = emailStr,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    val phoneStr = currentUser?.phoneNumber
                    if (!phoneStr.isNullOrEmpty()) {
                        Text(
                            text = phoneStr,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bio Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FA))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF3B7DD8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentUser?.status ?: "Hey there! I am using AmBle.",
                                fontSize = 12.sp,
                                color = Color(0xFF1B2A5E)
                            )
                        }
                    }

                    // Online / Offline Status Toggle Card
                    val isOnline = currentUser?.isOnline == true
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFECEFF1)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setUserOnlineStatus(!isOnline)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0xFF3ECF8E) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isOnline) "Status: Online" else "Status: Offline",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnline) Color(0xFF1B5E20) else Color(0xFF37474F)
                                    )
                                    Text(
                                        text = if (isOnline) "Visible to contacts as active" else "Appears offline to contacts",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { newValue ->
                                    viewModel.setUserOnlineStatus(newValue)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Save Login Info Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val newValue = !isRememberLogin
                                isRememberLogin = newValue
                                viewModel.setRememberLoginEnabled(newValue)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                tint = Color(0xFF3B7DD8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Save Login Info",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1B2A5E)
                                )
                                Text(
                                    text = "Stay logged in across app opens",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Switch(
                            checked = isRememberLogin,
                            onCheckedChange = { newValue ->
                                isRememberLogin = newValue
                                viewModel.setRememberLoginEnabled(newValue)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Glass Theme Presets Selector Card
                    GlassPresetThemeSelectorCard(viewModel = viewModel)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Storage & Auto-Clear Settings Card (QoL Feature)
                    StorageAutoClearSettingsCard(viewModel = viewModel)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Replay 3D Splash Animation Action Button
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            viewModel.navigateTo("logo_reveal")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2B52E1))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF2B52E1),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Replay 3D Intro Splash 🎬", color = Color(0xFF1B2A5E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Roll Update Option for Account & Profile
                    UserAppUpdateCheckCard(viewModel = viewModel)



                    if (currentUser?.isAdmin == true) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Supabase Sync Action Button (Visible for Admin ONLY)
                        OutlinedButton(
                            onClick = onOpenSupabase,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF3ECF8E))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3ECF8E))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Supabase Cloud Settings", color = Color(0xFF1B2A5E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Admin Roll Update Dispatcher Control Center
                        AdminInAppUpdateCard(viewModel = viewModel)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Log Out / Sign Out Option Button (Visible for ALL Users)
                    Button(
                        onClick = { showConfirmSignOut = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Log Out",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log Out / Sign Out",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showConfirmSignOut) {
        AlertDialog(
            onDismissRequest = { showConfirmSignOut = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out Confirmation", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                }
            },
            text = { Text("Are you sure you want to log out of your account? You will need to sign in again to access your messages.", color = Color.Gray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSignOut = false
                        onDismiss()
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSignOut = false }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun InAppUpdateBanner(viewModel: ChatViewModel) {
    val updateState by viewModel.updateState.collectAsState()

    when (val state = updateState) {
        is UpdateState.Available -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update Available: ${state.versionName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (state.isImmediate) {
                            Surface(
                                color = Color(0xFFFF4D4D).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "REQUIRED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF4D4D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = state.releaseNotes,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.startUpdate() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Now", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        is UpdateState.Downloading -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloading Update...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${state.percent}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF38BDF8),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        is UpdateState.Downloaded -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Ready to Install",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Tap restart to complete update.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = { viewModel.completeUpdate() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Restart", color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        else -> {
            // Idle or Error state
        }
    }
}

@Composable
fun AdminInAppUpdateCard(viewModel: ChatViewModel) {
    var versionName by remember { mutableStateOf("v1.3.0") }
    var releaseNotes by remember { mutableStateOf("New features synced directly from Google AI Studio AmBle project section.") }
    var isImmediate by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Google AI Studio Direct Rollout (AmBle)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Google AI Studio Auto Sync Status Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Admin Trial Active — Syncing from AI Studio",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Text(
                text = "Updates pushed directly via Google AI Studio are tested by Admin in Trial Mode, then rolled out to all users.",
                fontSize = 11.sp,
                color = Color.LightGray
            )

            OutlinedTextField(
                value = versionName,
                onValueChange = { versionName = it },
                label = { Text("Release Version", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.Gray
                )
            )

            OutlinedTextField(
                value = releaseNotes,
                onValueChange = { releaseNotes = it },
                label = { Text("Release Notes / Features", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isImmediate) "Mandatory Immediate Rollout" else "Flexible Background Rollout",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Switch(
                    checked = isImmediate,
                    onCheckedChange = { isImmediate = it }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Test in Trial Mode (Admin local test)
                OutlinedButton(
                    onClick = {
                        viewModel.triggerFlexibleUpdateDemo()
                        statusMessage = "Trial Mode Active: Testing Google AI Studio update locally..."
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🧪 Test Trial", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                // Button 2: Roll Out to All Users (Publish)
                Button(
                    onClick = {
                        viewModel.pushAdminUpdate(
                            versionName = versionName,
                            isImmediate = isImmediate,
                            releaseNotes = releaseNotes
                        )
                        statusMessage = "🚀 Success! New Update $versionName rolled out to all users!"
                    },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🚀 Roll Out to All Users", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    fontSize = 11.sp,
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun UserAppUpdateCheckCard(viewModel: ChatViewModel) {
    val updateState by viewModel.updateState.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Roll Update / App Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B2A5E)
                )
                Text(
                    text = when (val state = updateState) {
                        is UpdateState.Available -> "✨ New Update Rolled Out by Admin: ${state.versionName}"
                        is UpdateState.Downloading -> "Downloading update (${state.percent}%)..."
                        is UpdateState.Downloaded -> "Update Downloaded! Tap to restart and apply."
                        else -> "Installed: v1.3.0 (Awaiting next Admin Rollout)"
                    },
                    fontSize = 12.sp,
                    color = if (updateState is UpdateState.Available) Color(0xFF0284C7) else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    when (updateState) {
                        is UpdateState.Available -> viewModel.startUpdate()
                        is UpdateState.Downloaded -> viewModel.completeUpdate()
                        else -> viewModel.checkForUpdates()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (updateState) {
                        is UpdateState.Available -> Color(0xFF0284C7)
                        is UpdateState.Downloaded -> Color(0xFF065F46)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    imageVector = when (updateState) {
                        is UpdateState.Downloaded -> Icons.Default.RestartAlt
                        is UpdateState.Available -> Icons.Default.Download
                        else -> Icons.Default.SystemUpdate
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (updateState) {
                        is UpdateState.Downloaded -> "Restart"
                        is UpdateState.Available -> "Download"
                        else -> "Roll Update"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GlassPresetThemeSelectorCard(viewModel: ChatViewModel) {
    val currentPreset by viewModel.glassPreset.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("glass_preset_selector_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.88f)
        ),
        border = BorderStroke(1.5.dp, currentPreset.sky.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = currentPreset.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Glass Aesthetic Presets",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmBleInk
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = currentPreset.sky.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "${currentPreset.iconEmoji} ${currentPreset.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentPreset.primaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Toggle dynamic glass palettes across entire UI",
                fontSize = 11.sp,
                color = AmBleInkSoft
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
            ) {
                items(GlassPreset.values()) { preset ->
                    val isSelected = preset == currentPreset
                    Card(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setGlassPreset(preset) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) preset.pale1 else Color.White
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) preset.primary else Color.LightGray.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color circle indicator
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(preset.primary)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${preset.iconEmoji} ${preset.displayName}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) preset.ink else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageAutoClearSettingsCard(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val autoClearEnabled by viewModel.isAutoClearOldMessagesEnabled.collectAsState()
    var isCleaning by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Auto-Clear Old Messages",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Delete unstarred messages older than 30 days",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                Switch(
                    checked = autoClearEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setAutoClearOldMessagesEnabled(enabled)
                        val msg = if (enabled) "Auto-clear >30 days enabled" else "Auto-clear disabled"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("auto_clear_messages_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    isCleaning = true
                    viewModel.clearMessagesOlderThan30Days { count ->
                        isCleaning = false
                        Toast.makeText(context, "Storage optimized: Cleared $count old messages", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("clear_old_messages_button"),
                shape = RoundedCornerShape(10.dp),
                enabled = !isCleaning,
                border = BorderStroke(1.dp, Color(0xFF2563EB))
            ) {
                if (isCleaning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF2563EB), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cleaning Local Storage...", fontSize = 12.sp, color = Color(0xFF2563EB))
                } else {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Messages Older Than 30 Days Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            var isExportingZip by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    isExportingZip = true
                    viewModel.exportEncryptedRoomDatabaseZip(context) { fileName, summaryText, backupFile ->
                        isExportingZip = false
                        Toast.makeText(context, "Exported to Downloads: $fileName", Toast.LENGTH_LONG).show()

                        if (backupFile != null && backupFile.exists()) {
                            try {
                                val authority = "${context.packageName}.fileprovider"
                                val uri: Uri = androidx.core.content.FileProvider.getUriForFile(context, authority, backupFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "AmBle Encrypted Room Database Backup")
                                    putExtra(Intent.EXTRA_TEXT, summaryText)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Save Backup to Google Drive / Local Storage"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("export_database_zip_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                enabled = !isExportingZip
            ) {
                if (isExportingZip) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating Encrypted Database ZIP...", fontSize = 12.sp, color = Color.White)
                } else {
                    Icon(Icons.Default.Archive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Room DB Backup (Encrypted ZIP)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun VoiceSearchDialog(
    onQuerySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var voiceInputText by remember { mutableStateOf("") }
    val quickPhrases = listOf("Alice", "Bob", "Project", "Design", "Meeting", "Support", "Finance", "Crypto")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Search",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Voice Search Chats", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Speak or select a phrase to filter", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Listening for voice input...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                OutlinedTextField(
                    value = voiceInputText,
                    onValueChange = { voiceInputText = it },
                    label = { Text("Voice Input Transcribed") },
                    placeholder = { Text("e.g. Alice or Meeting") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF2B52E1)) }
                )

                Text("Quick Voice Suggestions:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickPhrases) { phrase ->
                        SuggestionChip(
                            onClick = {
                                onQuerySelected(phrase)
                            },
                            label = { Text("🎙️ $phrase") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (voiceInputText.isNotBlank()) {
                        onQuerySelected(voiceInputText.trim())
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B52E1))
            ) {
                Text("Apply Voice Filter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

