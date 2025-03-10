import axios from 'axios';
import { ToDoItem, NewToDoItem, UpdateToDoItem } from '../types/ToDoItem';

const API_BASE_URL = '/todolist';

const todoService = {
  getAllItems: async (): Promise<ToDoItem[]> => {
    const response = await axios.get(API_BASE_URL);
    return response.data;
  },
  
  getItemById: async (id: number): Promise<ToDoItem> => {
    const response = await axios.get(`${API_BASE_URL}/${id}`);
    return response.data;
  },
  
  addItem: async (item: NewToDoItem): Promise<number> => {
    const response = await axios.post(API_BASE_URL, item);
    const locationHeader = response.headers.location;
    return locationHeader ? parseInt(locationHeader) : 0;
  },
  
  updateItem: async (id: number, item: UpdateToDoItem): Promise<ToDoItem> => {
    const response = await axios.put(`${API_BASE_URL}/${id}`, item);
    return response.data;
  },
  
  deleteItem: async (id: number): Promise<boolean> => {
    await axios.delete(`${API_BASE_URL}/${id}`);
    return true;
  }
};

export default todoService;