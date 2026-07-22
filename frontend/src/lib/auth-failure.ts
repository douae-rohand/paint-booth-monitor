let authFailureHandler: (() => Promise<void> | void) | null = null;

export function registerAuthFailureHandler(handler: () => Promise<void> | void) {
  authFailureHandler = handler;
}

export async function handleAuthFailure() {
  if (authFailureHandler) {
    await authFailureHandler();
  }
}
