import { TestBed } from '@angular/core/testing';

import { TodoApi } from './todo-api';

describe('TodoApi', () => {
  let service: TodoApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TodoApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
