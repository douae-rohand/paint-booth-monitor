class AlertingService:
    # Service to check absolute and dynamic thresholds
    def check_thresholds(self, value: float, threshold: float):
        return value > threshold
