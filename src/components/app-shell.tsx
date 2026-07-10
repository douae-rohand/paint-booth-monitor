import { Link, useRouterState } from "@tanstack/react-router";
import { LayoutDashboard, History, Factory } from "lucide-react";
import type { ReactNode } from "react";

const nav = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/history", label: "Historique", icon: History },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const today = new Date().toLocaleDateString("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

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
              item.to === "/" ? pathname === "/" : pathname.startsWith(item.to);
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
        </header>

        <main className="min-w-0 flex-1 px-6 pb-10 lg:px-10">{children}</main>
      </div>
    </div>
  );
}

