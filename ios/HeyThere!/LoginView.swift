import SwiftUI
import FirebaseAuth

struct LoginView: View {
    @State private var email = ""
    @State private var password = ""
    @State private var isSigningUp = false
    @State private var errorMessage = ""
    @State private var successMessage = ""
    @State private var showAlert = false

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            
            Text("HeyThere")
                .font(.system(size: 44, weight: .black))
                .foregroundColor(.orange)
            
            Text(isSigningUp ? "Create an account to get started" : "Welcome back")
                .font(.subheadline)
                .foregroundColor(.gray)

            VStack(alignment: .leading, spacing: 15) {
                TextField("Email", text: $email)
                    .textFieldStyle(.roundedBorder)
                    .autocapitalization(.none)
                    .keyboardType(.emailAddress)
                
                SecureField("Password", text: $password)
                    .textFieldStyle(.roundedBorder)
                
                if !isSigningUp {
                    HStack {
                        Spacer()
                        Button("Forgot Password?") {
                            resetPassword()
                        }
                        .font(.footnote)
                        .foregroundColor(.orange)
                    }
                }
            }
            .padding(.horizontal)

            if !errorMessage.isEmpty {
                Text(errorMessage).foregroundColor(.red).font(.caption).padding(.horizontal).multilineTextAlignment(.center)
            }

            Button(action: isSigningUp ? signUp : login) {
                Text(isSigningUp ? "Sign Up" : "Log In")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.orange)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                    .bold()
            }
            .padding(.horizontal)

            Button(action: { isSigningUp.toggle(); errorMessage = "" }) {
                Text(isSigningUp ? "Already have an account? Log In" : "New here? Create an Account")
                    .font(.footnote)
                    .foregroundColor(.orange)
            }
            Spacer()
        }
        .alert(isPresented: $showAlert) {
            Alert(title: Text("Email Sent"), message: Text(successMessage), dismissButton: .default(Text("OK")))
        }
    }

    func login() {
        Auth.auth().signIn(withEmail: email, password: password) { _, error in
            if let error = error { self.errorMessage = error.localizedDescription }
        }
    }

    func signUp() {
        Auth.auth().createUser(withEmail: email, password: password) { _, error in
            if let error = error { self.errorMessage = error.localizedDescription }
        }
    }

    func resetPassword() {
        guard !email.isEmpty else { self.errorMessage = "Enter email first."; return }
        Auth.auth().sendPasswordReset(withEmail: email) { error in
            if let error = error { self.errorMessage = error.localizedDescription }
            else {
                self.successMessage = "Check your inbox for a reset link."
                self.showAlert = true
            }
        }
    }
}
