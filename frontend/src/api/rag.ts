import client from './client';

interface ChatbotRequest {
  question: string;
}

interface ChatbotResponse {
  answer: string;
  sources?: string[];
}

export const askChatbot = async (question: string): Promise<ChatbotResponse> => {
  const response = await client.post<ChatbotResponse>('/api/rag/ask', { question });
  return response.data;
};
