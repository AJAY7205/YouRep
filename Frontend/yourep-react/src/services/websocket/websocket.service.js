import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const WS_URL = 'http://localhost:8080/ws';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
  }

  connect(uploadId, onProgress, onComplete, onError) {
    if (this.client?.connected) {
      this.disconnect();
    }

    const socket = new SockJS(WS_URL);
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        const subscription = this.client.subscribe(
          `/topic/upload-progress/${uploadId}`,
          (message) => {
            const progress = JSON.parse(message.body);

            if (progress.status === 'COMPLETED') {
              if (onComplete) onComplete(progress);
            } else if (progress.status === 'FAILED') {
              if (onError) onError(progress.error || 'Upload failed');
            } else {
              if (onProgress) onProgress(progress);
            }
          }
        );
        this.subscriptions.set(uploadId, subscription);
      },
      onStompError: (frame) => {
        if (onError) onError(frame.headers?.message || 'WebSocket error');
      },
    });

    this.client.activate();
  }

  disconnect() {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }
}

const websocketService = new WebSocketService();
export default websocketService;
