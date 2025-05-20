const socket = new WebSocket("ws://localhost:8080/ws/stats");

socket.onmessage = function (event) {
    document.getElementById("estatisticas").innerText = event.data;
};
