/**
 * Green Coin - Configuration
 * 
 * Environment variables loaded from Vercel
 */

// Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyD6IKDq9oTQ1Vyn7bw-3iAjvfTvaukvhag",
    authDomain: "greencoin-bc425.firebaseapp.com",
    projectId: "greencoin-bc425",
    storageBucket: "greencoin-bc425.firebasestorage.app",
    messagingSenderId: "889071300439",
    appId: "1:889071300439:web:194b95ba8c84e49611d812",
    measurementId: "G-CW44XPQF3Q"
};

// Backend API URL
const API_BASE_URL = "http://localhost:8080";  // Change to production URL for Vercel

// WebSocket URL
const WS_URL = "http://localhost:8080/ws";

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Export for other modules
window.config = {
    API_BASE_URL,
    WS_URL
};
