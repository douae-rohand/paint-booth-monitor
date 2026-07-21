import client from './client';

export const askChatbot = async (question) => {
  const response = await client.post('/api/rag/ask', { question });
  return response.data;
};
