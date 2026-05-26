import SwiftUI
import FirebaseAuth

struct ChatDetailView: View {
    let recipientId: String
    let recipientName: String
    @StateObject var chatManager = ChatManager()
    @State private var messageText = ""

    var body: some View {
        VStack(spacing: 0) {
            // 1. Message History
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(chatManager.messages) { msg in
                            ChatBubble(message: msg)
                                // Long-press to delete
                                .contextMenu {
                                    if msg.senderId == Auth.auth().currentUser?.uid {
                                        Button(role: .destructive) {
                                            if let id = msg.id {
                                                chatManager.deleteMessage(messageId: id)
                                            }
                                        } label: {
                                            Label("Delete Message", systemImage: "trash")
                                        }
                                    }
                                }
                                .id(msg.id) // Needed for auto-scroll
                        }
                    }
                    .padding(.top, 10)
                }
                // NEW iOS 17+ SYNTAX
                .onChange(of: chatManager.messages.count) { oldValue, newValue in
                    withAnimation {
                        proxy.scrollTo(chatManager.messages.last?.id, anchor: .bottom)
                    }
                }
            }

            Divider()

            // 2. Typing Bar (The Input Area)
            HStack(spacing: 12) {
                TextField("Message...", text: $messageText, axis: .vertical)
                    .padding(10)
                    .background(Color.gray.opacity(0.1))
                    .cornerRadius(20)
                    .lineLimit(1...5) // Allows the box to grow slightly if typing a long message
                
                Button(action: {
                    if !messageText.trimmingCharacters(in: .whitespaces).isEmpty {
                        chatManager.sendMessage(to: recipientId, text: messageText)
                        messageText = ""
                    }
                }) {
                    Image(systemName: "paperplane.fill")
                        .font(.system(size: 22))
                        .foregroundColor(.orange)
                }
                .disabled(messageText.isEmpty)
            }
            .padding(.horizontal)
            .padding(.vertical, 10)
            .background(Color(UIColor.systemBackground))
        }
        .navigationTitle(recipientName)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            chatManager.listenForMessages()
        }
        .onDisappear {
            // This stops the repeat bug by killing the listener when you exit the chat
            chatManager.stopListening()
        }
    }
}

// Custom Chat Bubble Component
struct ChatBubble: View {
    let message: Message
    var isMe: Bool { message.senderId == Auth.auth().currentUser?.uid }

    var body: some View {
        HStack {
            if isMe { Spacer() }
            
            VStack(alignment: isMe ? .trailing : .leading) {
                Text(message.text)
                    .padding(12)
                    .background(isMe ? Color.orange : Color.gray.opacity(0.2))
                    .foregroundColor(isMe ? .white : .primary)
                    .cornerRadius(18)
                
                Text(message.timestamp, style: .time)
                    .font(.system(size: 10))
                    .foregroundColor(.gray)
                    .padding(.horizontal, 4)
            }
            
            if !isMe { Spacer() }
        }
        .padding(.horizontal, 10)
    }
}
