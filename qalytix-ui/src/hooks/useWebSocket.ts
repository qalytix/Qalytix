import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../stores/authStore'
import type { BuildEvent } from '../types/dashboard'

interface Options {
  orgId: number
  onBuildEvent: (event: BuildEvent) => void
}

export function useWebSocket({ orgId, onBuildEvent }: Options) {
  const accessToken = useAuthStore((s) => s.accessToken)
  const clientRef   = useRef<Client | null>(null)

  const onBuildEventRef = useRef(onBuildEvent)
  onBuildEventRef.current = onBuildEvent

  const connect = useCallback(() => {
    if (!accessToken) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/org/${orgId}/builds`, (msg) => {
          try {
            const event: BuildEvent = JSON.parse(msg.body)
            onBuildEventRef.current(event)
          } catch {
            // malformed message — ignore
          }
        })
      },
      onStompError: (frame) => {
        console.warn('STOMP error', frame.headers['message'])
      },
    })

    client.activate()
    clientRef.current = client
  }, [accessToken, orgId])

  useEffect(() => {
    connect()
    return () => {
      clientRef.current?.deactivate()
      clientRef.current = null
    }
  }, [connect])
}
