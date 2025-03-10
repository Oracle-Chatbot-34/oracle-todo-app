import React from 'react';
import { motion } from 'framer-motion';
import { Button } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import Moment from 'react-moment';
import { ToDoItem } from '../types/ToDoItem';

interface TodoItemProps {
  item: ToDoItem;
  toggleDone: (event: React.MouseEvent, id: number, description: string, done: boolean) => void;
  deleteItem: (id: number) => void;
}

const TodoItem: React.FC<TodoItemProps> = ({ item, toggleDone, deleteItem }) => {
  return (
    <motion.tr 
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, height: 0 }}
      transition={{ duration: 0.3 }}
      className="hover:bg-gray-800"
    >
      <td className="description">{item.description}</td>
      <td className="date">
        <Moment format="MMM Do hh:mm:ss">{item.createdAt}</Moment>
      </td>
      <td>
        <Button 
          variant="contained" 
          className="DoneButton" 
          onClick={(event) => toggleDone(event, item.id, item.description, !item.done)} 
          size="small"
        >
          {item.done ? 'Undo' : 'Done'}
        </Button>
      </td>
      {item.done && (
        <td>
          <Button 
            startIcon={<DeleteIcon />} 
            variant="contained" 
            className="DeleteButton" 
            onClick={() => deleteItem(item.id)} 
            size="small"
          >
            Delete
          </Button>
        </td>
      )}
    </motion.tr>
  );
}

export default TodoItem;