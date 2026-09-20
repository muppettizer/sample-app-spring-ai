import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { ToastrService } from 'ngx-toastr';

import { ScreeningChatApi } from './screening-chat-api';
import type {
  GridUpdate,
  ScreeningChatMessage,
  ScreeningChatResponse
} from './models';

interface ChatSessionTab {
  chatSessionId: string;
  label: string;
}

@Component({
  selector: 'app-screening-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './screening-chat.html',
  styleUrl: './screening-chat.scss'
})
export class ScreeningChatComponent implements OnInit {
  @Input() portfolioManagerId = 'pm-001';
  @Input() gridApi: any;
  @Input() selectedRowCount = signal(0);
  @Input() getStructuredSchemaSafe: () => any = () => ({ columns: [] });
  @Input() applyResult: (result: GridUpdate) => void = () => {};

  chatSessions: ChatSessionTab[] = [];
  activeChatSession: ChatSessionTab | null = null;

  chatInput = '';
  chatLoading = signal(false);
  chatMessages: ScreeningChatMessage[] = [];

  constructor(
    private screeningChatApi: ScreeningChatApi,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadChatSessions();
  }

  private loadChatSessions(): void {
    this.screeningChatApi
      .getSessions(this.portfolioManagerId)
      .subscribe({
        next: (sessions) => {
          const chatSessions = this.toChatSessionTabs(sessions);

          if (chatSessions.length === 0) {
            this.createInitialChatSession();
            return;
          }

          this.chatSessions = chatSessions;
          this.activeChatSession = chatSessions[0] ?? null;
          this.loadChatHistory();
        },
        error: (error) => {
          console.error('Failed to load chat sessions', error);

          this.toastr.error(
            'Failed to load chat sessions',
            'Chat error'
          );
        }
      });
  }

  private toChatSessionTabs(
    sessions?: { chatSessionId: string }[] | null
  ): ChatSessionTab[] {
    return (sessions ?? []).map((session, index) => ({
      chatSessionId: session.chatSessionId,
      label: `Chat ${index + 1}`
    }));
  }

  private createInitialChatSession(): void {
    this.screeningChatApi
      .createSession(this.portfolioManagerId)
      .subscribe({
        next: ({ chatSessionId }) => {
          const session: ChatSessionTab = {
            chatSessionId,
            label: 'Chat 1'
          };

          this.chatSessions = [session];
          this.activeChatSession = session;
          this.chatMessages = [];
        },
        error: (error) => {
          console.error(
            'Failed to create initial chat session',
            error
          );

          this.toastr.error(
            'Failed to create chat session',
            'Chat session error'
          );
        }
      });
  }

  startNewChatSession(): void {
    this.screeningChatApi
      .createSession(this.portfolioManagerId)
      .subscribe({
        next: ({ chatSessionId }) => {
          const session: ChatSessionTab = {
            chatSessionId,
            label: `Chat ${this.chatSessions.length + 1}`
          };

          this.chatSessions = [
            ...this.chatSessions,
            session
          ];

          this.activeChatSession = session;
          this.chatMessages = [];
          this.chatInput = '';
        },
        error: (error) => {
          console.error(
            'Failed to create new chat session',
            error
          );

          this.toastr.error(
            'Failed to create a new chat session',
            'Chat session error'
          );
        }
      });
  }

  switchChatSession(session: ChatSessionTab): void {
    if (this.activeChatSession === session) {
      return;
    }

    this.activeChatSession = session;
    this.chatMessages = [];
    this.chatInput = '';

    this.loadChatHistory();
  }

  clearChatHistory(): void {
    const session = this.activeChatSession;

    if (!session) {
      return;
    }

    this.screeningChatApi
      .clearScreeningChatHistory(
        this.portfolioManagerId,
        session.chatSessionId
      )
      .subscribe({
        next: () => {
          this.chatMessages = [];
          this.chatInput = '';
        },
        error: (error) => {
          console.error(
            'Failed to clear chat messages',
            error
          );

          this.toastr.error(
            'Failed to clear chat messages',
            'Chat error'
          );
        }
      });
  }

  runPreset(preset: string): void {
    if (!this.selectedRowCount() || this.chatLoading()) {
      return;
    }

    this.sendMessage(preset);
  }

  sendScreeningChat(): void {
    const message = this.chatInput.trim();

    if (!message || this.chatLoading()) {
      return;
    }

    this.sendMessage(message);
  }

  private sendMessage(message: string): void {
    const session = this.activeChatSession;

    if (!session) {
      return;
    }

    this.chatLoading.set(true);

    this.screeningChatApi
      .screeningChat(
        this.portfolioManagerId,
        session.chatSessionId,
        message,
        this.gridApi?.getState?.() ?? {},
        this.getStructuredSchemaSafe(),
        this.gridApi?.getSelectedRows?.() ?? []
      )
      .pipe(
        finalize(() => this.chatLoading.set(false))
      )
      .subscribe({
        next: (response: ScreeningChatResponse) => {
          this.chatMessages = response?.history ?? [];
          this.chatInput = '';

          if (response?.gridUpdate) {
            this.applyResult(response.gridUpdate);

            this.toastr.success(
              'Grid updated based on AI screening',
              'Grid Updated'
            );
          }
        },
        error: (error) => {
          console.error(
            'Failed to send screening chat message',
            error
          );

          this.toastr.error(
            'Failed to send screening chat message',
            'Chat error'
          );
        }
      });
  }

  private loadChatHistory(): void {
    const session = this.activeChatSession;

    if (!session) {
      this.chatMessages = [];
      return;
    }

    this.screeningChatApi
      .getScreeningChatHistory(
        this.portfolioManagerId,
        session.chatSessionId
      )
      .subscribe({
        next: (response) => {
          this.chatMessages = response?.messages ?? [];
        },
        error: (error) => {
          console.error(
            'Failed to load chat history',
            error
          );

          this.chatMessages = [];

          this.toastr.error(
            'Failed to load chat history',
            'Chat error'
          );
        }
      });
  }
}
