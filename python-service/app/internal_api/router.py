from fastapi import APIRouter

router = APIRouter(prefix="/internal/v1")

@router.get("/status")
def get_internal_status():
    return {"status": "internal_ok"}
