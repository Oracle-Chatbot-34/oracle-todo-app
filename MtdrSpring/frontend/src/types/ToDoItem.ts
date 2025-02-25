export interface ToDoItem {
    id: number;
    description: string;
    createdAt: string;
    done: boolean;
  }
  
  export interface NewToDoItem {
    description: string;
  }
  
  export interface UpdateToDoItem {
    description?: string;
    done?: boolean;
  }