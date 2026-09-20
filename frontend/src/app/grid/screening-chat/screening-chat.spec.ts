import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ScreeningChatComponent } from './screening-chat';

describe('ScreeningChatComponent', () => {
  let component: ScreeningChatComponent;
  let fixture: ComponentFixture<ScreeningChatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScreeningChatComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ScreeningChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
