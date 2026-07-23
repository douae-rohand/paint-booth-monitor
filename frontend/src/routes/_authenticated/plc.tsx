import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/plc')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_authenticated/plc"!</div>
}
