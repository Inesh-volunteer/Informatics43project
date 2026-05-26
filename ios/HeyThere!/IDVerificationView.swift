//
//  IDVerificationView.swift
//  HeyThere!
//
//  Created by Sumeet Agarwal on 5/25/26.
//


import SwiftUI
import FirebaseStorage
import FirebaseFirestore
import FirebaseAuth

struct IDVerificationView: View {
    @State private var idImage: UIImage?
    @State private var isPickerPresented = false
    @State private var uploadProgress: Double = 0
    @State private var isUploading = false
    @State private var statusMessage = "Please upload a photo of your Driver's License, Passport, or State ID."

    var body: some View {
        VStack(spacing: 20) {
            Text("Verify Your Identity")
                .font(.title2).bold()
            
            Text(statusMessage)
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            // Image Preview Area
            ZStack {
                RoundedRectangle(cornerRadius: 15)
                    .stroke(Color.orange.opacity(0.5), lineWidth: 2)
                    .frame(height: 200)
                
                if let image = idImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .cornerRadius(15)
                } else {
                    Image(systemName: "person.badge.shield.check")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                }
            }
            .padding()
            .onTapGesture { isPickerPresented = true }

            if isUploading {
                ProgressView("Uploading...", value: uploadProgress, total: 1.0)
                    .padding()
            }

            Button(action: uploadID) {
                Text("Submit for Verification")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(idImage == nil || isUploading ? Color.gray : Color.orange)
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
            .disabled(idImage == nil || isUploading)
            .padding(.horizontal)
        }
        .sheet(isPresented: $isPickerPresented) {
            ImagePicker(image: $idImage)
        }
    }

    func uploadID() {
        guard let uid = Auth.auth().currentUser?.uid, let image = idImage else { return }
        guard let imageData = image.jpegData(compressionQuality: 0.5) else { return }

        isUploading = true
        let storageRef = Storage.storage().reference().child("id_verifications/\(uid).jpg")

        let uploadTask = storageRef.putData(imageData, metadata: nil) { metadata, error in
            if let error = error {
                statusMessage = "Upload failed: \(error.localizedDescription)"
                isUploading = false
                return
            }
            
            // Once uploaded, mark the user as "Pending Verification" in Firestore
            let db = Firestore.firestore(database: "users")
            db.collection("users").document(uid).updateData([
                "isIDVerified": "pending",
                "idUploadDate": Timestamp(date: Date())
            ])
            
            statusMessage = "✅ Submitted! Our team will review your ID shortly."
            isUploading = false
        }

        // Track progress
        uploadTask.observe(.progress) { snapshot in
            uploadProgress = snapshot.progress?.fractionCompleted ?? 0
        }
    }
}

struct ImagePicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: ImagePicker
        init(_ parent: ImagePicker) { self.parent = parent }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let uiImage = info[.originalImage] as? UIImage {
                parent.image = uiImage
            }
            picker.dismiss(animated: true)
        }
    }
}
