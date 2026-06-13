async function executeLogin() {
    const userField = document.getElementById('username').value;
    const passField = document.getElementById('password').value;
    const errorDiv = document.getElementById('errorMsg');

    // Hide error message on new attempt
    errorDiv.style.display = "none";

    try {
        // Call the perfectly working login function from api.js
        const response = await api.login(userField, passField);

        // api.js now returns { success: true } and handles saving the token!
        if (response.success) {
            // Instantly teleport to the vault dashboard!
            window.location.href = "vault.html";
        } else {
            errorDiv.innerText = "Invalid credentials. Please try again.";
            errorDiv.style.display = "block";
        }
    } catch (error) {
        console.error(error);
        // Show the actual error thrown by api.js
        errorDiv.innerText = error.message || "Could not connect to the server.";
        errorDiv.style.display = "block";
    }
}