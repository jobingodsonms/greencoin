/**
 * Green Coin - Authentication Logic
 * 
 * Handles Firebase authentication and backend registration
 */

const auth = firebase.auth();

// Show/hide loading spinner
function showLoading(show) {
    document.getElementById('loadingSpinner').style.display = show ? 'flex' : 'none';
}

// Show error message
function showError(message) {
    const errorEl = document.getElementById('errorMessage');
    errorEl.textContent = message;
    errorEl.style.display = 'block';
    setTimeout(() => {
        errorEl.style.display = 'none';
    }, 5000);
}

// Register user with backend
async function registerUserWithBackend(idToken, displayName) {
    try {
        const response = await fetch(`${config.API_BASE_URL}/api/user/register?displayName=${encodeURIComponent(displayName || '')}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${idToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to register with backend');
        }

        const userData = await response.json();
        return userData;
    } catch (error) {
        console.error('Backend registration error:', error);
        throw error;
    }
}

// Redirect based on user role
function redirectToDashboard(role) {
    switch (role) {
        case 'CITIZEN':
            window.location.href = 'citizen.html';
            break;
        case 'COLLECTOR':
            window.location.href = 'collector.html';
            break;
        case 'AUTHORITY':
            window.location.href = 'authority.html';
            break;
        default:
            window.location.href = 'citizen.html';
    }
}

// Google Sign-In Handler
document.getElementById('googleSignInBtn')?.addEventListener('click', async () => {
    showLoading(true);

    try {
        const provider = new firebase.auth.GoogleAuthProvider();
        const result = await auth.signInWithPopup(provider);

        // Get Firebase ID token
        const idToken = await result.user.getIdToken();

        // Register with backend
        const userData = await registerUserWithBackend(idToken, result.user.displayName);

        // Store user data and token
        localStorage.setItem('userProfile', JSON.stringify(userData));
        localStorage.setItem('idToken', idToken);

        // Redirect to dashboard
        redirectToDashboard(userData.role);

    } catch (error) {
        console.error('Google sign-in error:', error);
        showError(error.message || 'Failed to sign in with Google');
        showLoading(false);
    }
});

// Email/Password Sign-In Handler
document.getElementById('emailLoginForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    showLoading(true);

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    try {
        const result = await auth.signInWithEmailAndPassword(email, password);

        // Get Firebase ID token
        const idToken = await result.user.getIdToken();

        // Register with backend
        const userData = await registerUserWithBackend(idToken, result.user.displayName || email.split('@')[0]);

        // Store user data and token
        localStorage.setItem('userProfile', JSON.stringify(userData));
        localStorage.setItem('idToken', idToken);

        // Redirect to dashboard
        redirectToDashboard(userData.role);

    } catch (error) {
        console.error('Email sign-in error:', error);
        showError(error.message || 'Invalid email or password');
        showLoading(false);
    }
});

// Check if user is already logged in
auth.onAuthStateChanged(async (user) => {
    if (user && window.location.pathname.includes('login.html')) {
        try {
            const idToken = await user.getIdToken();
            const userData = await registerUserWithBackend(idToken, user.displayName);

            localStorage.setItem('userProfile', JSON.stringify(userData));
            localStorage.setItem('idToken', idToken);

            redirectToDashboard(userData.role);
        } catch (error) {
            console.error('Auto-login error:', error);
        }
    }
});
