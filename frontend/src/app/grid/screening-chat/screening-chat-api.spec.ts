import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ScreeningChatApi } from './screening-chat-api';

describe('ScreeningChatApi', () => {
  let service: ScreeningChatApi;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ScreeningChatApi]
    });
    service = TestBed.inject(ScreeningChatApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
