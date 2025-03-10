import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CircularProgress, TableBody } from '@mui/material';
import todoService from './services/todoService';
import { ToDoItem } from './types/ToDoItem';
import NewItem from './components/NewItem';
import TodoItem from './components/TodoItem';

function App() {
  const [isLoading, setLoading] = useState<boolean>(false);
  const [isInserting, setInserting] = useState<boolean>(false);
  const [items, setItems] = useState<ToDoItem[]>([]);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    loadItems();
  }, []);

  const loadItems = async () => {
    try {
      setLoading(true);
      const data = await todoService.getAllItems();
      setItems(data);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('An unknown error occurred'));
    } finally {
      setLoading(false);
    }
  };

  const addItem = async (text: string) => {
    try {
      setInserting(true);
      const id = await todoService.addItem({ description: text });
      const newItem: ToDoItem = {
        id,
        description: text,
        createdAt: new Date().toISOString(),
        done: false
      };
      setItems([newItem, ...items]);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to add item'));
    } finally {
      setInserting(false);
    }
  };

  const toggleDone = async (event: React.MouseEvent, id: number, description: string, done: boolean) => {
    event.preventDefault();
    try {
      await todoService.updateItem(id, { description, done });
      setItems(items.map(item => 
        item.id === id ? { ...item, done } : item
      ));
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to update item'));
    }
  };

  const deleteItem = async (id: number) => {
    try {
      await todoService.deleteItem(id);
      setItems(items.filter(item => item.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to delete item'));
    }
  };

  return (
    <div className="App">
      <motion.h1 
        className="text-2xl font-bold mb-4"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        MY TODO LIST
      </motion.h1>
      
      <NewItem addItem={addItem} isInserting={isInserting} />
      
      {error && (
        <div className="w-full p-3 mb-4 bg-red-600 text-white rounded">
          Error: {error.message}
        </div>
      )}
      
      {isLoading ? (
        <div className="flex justify-center my-8">
          <CircularProgress />
        </div>
      ) : (
        <div id="maincontent" className="w-full">
          <table id="itemlistNotDone" className="itemlist">
            <TableBody>
              <AnimatePresence>
                {items
                  .filter(item => !item.done)
                  .map(item => (
                    <TodoItem 
                      key={item.id} 
                      item={item} 
                      toggleDone={toggleDone} 
                      deleteItem={deleteItem} 
                    />
                  ))
                }
              </AnimatePresence>
            </TableBody>
          </table>
          
          <motion.h2 
            id="donelist"
            className="font-bold mt-6 mb-2"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
          >
            Done items
          </motion.h2>
          
          <table id="itemlistDone" className="itemlist">
            <TableBody>
              <AnimatePresence>
                {items
                  .filter(item => item.done)
                  .map(item => (
                    <TodoItem 
                      key={item.id} 
                      item={item} 
                      toggleDone={toggleDone} 
                      deleteItem={deleteItem} 
                    />
                  ))
                }
              </AnimatePresence>
            </TableBody>
          </table>
        </div>
      )}
    </div>
  );
}

export default App;