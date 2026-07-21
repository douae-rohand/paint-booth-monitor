from fastapi import FastAPI

app = FastAPI(
    title="Python Data & Intelligence Service",
    description="Service for PLC communication, AI predictions, alerts and RAG chatbot features"
)

@app.get("/")
def read_root():
    return {"status": "ok", "service": "python-service"}
