import React, { useState } from "react";
import { motion } from "framer-motion";
import { Button } from '@mui/material';

interface NewItemProps {
  addItem: (text: string) => void;
  isInserting: boolean;
}

function NewItem({ addItem, isInserting }: NewItemProps) {
  const [item, setItem] = useState('');
  
  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!item.trim()) {
      return;
    }
    addItem(item);
    setItem("");
  }
  
  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setItem(e.target.value);
  }
  
  return (
    <div id="newinputform" className="w-full mb-6">
      <form className="flex flex-row items-center">
        <motion.input
          id="newiteminput"
          placeholder="New item"
          type="text"
          autoComplete="off"
          value={item}
          onChange={handleChange}
          onKeyDown={event => {
            if (event.key === 'Enter') {
              handleSubmit(event);
            }
          }}
          whileFocus={{ scale: 1.01 }}
          transition={{ duration: 0.2 }}
        />
        <span className="mx-2"></span>
        <motion.div 
          whileHover={{ scale: 1.05 }} 
          whileTap={{ scale: 0.95 }}
        >
          <Button
            className="AddButton"
            variant="contained"
            disabled={isInserting}
            onClick={!isInserting ? (e) => handleSubmit(e) : undefined}
            size="small"
          >
            {isInserting ? 'Adding…' : 'Add'}
          </Button>
        </motion.div>
      </form>
    </div>
  );
}

export default NewItem;