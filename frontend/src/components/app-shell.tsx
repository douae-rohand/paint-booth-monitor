import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import { History, LayoutDashboard, Factory, LogOut } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";

const nav = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/history", label: "Historique", icon: History },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const today = new Date().toLocaleDateString("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

  const handleLogout = async () => {
    await logout();
    navigate({ to: "/login" });
  };

  return (
    <div className="flex min-h-screen w-full">
      {/* Sidebar */}
      <aside className="sticky top-0 flex h-screen w-20 shrink-0 flex-col items-center gap-2 py-6 lg:w-24">
        <div className="neu-card-sm mb-4 flex h-12 w-12 items-center justify-center rounded-2xl">
          <Factory className="h-6 w-6 text-primary" />
        </div>
        <nav className="flex flex-col items-center gap-1 rounded-3xl border border-border bg-white p-2">
          {nav.map((item) => {
            const active =
              item.to === "/dashboard" ? pathname === "/dashboard" : pathname.startsWith(item.to);
            const Icon = item.icon;
            return (
              <Link
                key={item.to}
                to={item.to}
                title={item.label}
                className={
                  "flex h-11 w-11 items-center justify-center rounded-2xl transition-all " +
                  (active
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:text-foreground hover:bg-secondary")
                }
              >
                <Icon className="h-5 w-5" />
              </Link>
            );
          })}
        </nav>
        <div className="mt-auto">
          <button
            type="button"
            onClick={handleLogout}
            title="Déconnexion"
            className="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary text-primary-foreground transition-all hover:opacity-90"
          >
            <LogOut className="h-5 w-5" />
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex flex-wrap items-center justify-between gap-4 px-6 py-6 lg:px-10">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">
              {today}
            </p>
            <h1 className="mt-1 text-2xl font-bold tracking-tight lg:text-3xl">
              Supervision des cabines de peinture
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Voici l'état des cabines de peinture aujourd'hui.
            </p>
          </div>
          <div className="flex items-center gap-3">
            {user?.email && (
              <span className="text-sm text-muted-foreground">
                {user.email}
              </span>
            )}
          </div>
        </header>

        <main className="min-w-0 flex-1 px-6 pb-10 lg:px-10">{children}</main>
      </div>
    </div>
  );
}

