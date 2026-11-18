// [ITERATION 2] Data shape matching Spring Boot TodoDto (GET /api/todos)
//   - id is a integer, so it arrives as a number
//   - userId is always null on the public /api/todos endpoint (read-only, ignored on POST/PUT)
export interface Todo {
  id: number;
  title: string;
  description?: string | null;
  completed: boolean;
  userId?: number | null;
}

// [ITERATION 3] Payload for CREATE (POST /api/todos). id and userId are set by the server.
export interface NewTodo {
  title: string;
  description?: string | null;
  completed?: boolean;
}
