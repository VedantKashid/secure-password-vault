async function executeLogin() {
    const userField = document.getElementById('username').value;
    const passField = document.getElementById('password').value;
    const errorDiv = document.getElementById('errorMsg');

    try {
        // Call the login function from our api.js class
        const response = await api.login(userField, passField);

        // Check if the backend gave us a JWT token
        // (Note: based on Day 5, your JWT is likely stored in response.data or response.token)
        if (response.token || response.data) {
            // Save token and redirect to the vault dashboard
            const tokenToSave = response.token || response.data;
            localStorage.setItem('token', tokenToSave);
            api.token = tokenToSave;

            window.location.href = "vault.html"; // We will build this page next!
        } else {
            errorDiv.style.display = "block";
        }
    } catch (error) {
        console.error(error);
        errorDiv.innerText = "Could not connect to the server.";
        errorDiv.style.display = "block";
    }
}