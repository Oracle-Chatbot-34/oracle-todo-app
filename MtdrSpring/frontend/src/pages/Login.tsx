import logo from '../assets/logo.png';
import { useState } from 'react';

export default function Login() {
    return(
        <div className="bg-background h-screen w-full flex flex-row items-center justify-center gap-[250px]">
            <div className="flex flex-col items-center gap-[20px]">
                <p className="text-[96px]">DashMaster</p>
                <img src={logo} className="w-[500px] h-[500px]" alt="Logo" />
            </div>
            
            <div className="w-[440px] h-[460px] bg-white rounded-lg shadow-xl">
                <br/>
                <div className="justify-center flex flex-col items-center">
                    <p className="text-[40px] w-[230px] text-sm/12 text-center">Go to your Dashboard!</p>
                </div>
                <br/>
                <div className="gap-[20px]">
                    <p className="text-[20px] text-[#747276] text-left">Email</p>
                    <input 
                        type="text" 
                        className="w-[300px] h-[40px] border-2 border-black rounded-lg m-3 p-4"
                    />
                    <p className="text-[20px] text-[#747276] text-left">Password</p>
                    <input type="password" className="w-[300px] h-[40px] border-2 border-gray-500 rounded-lg"/>
                </div>

            </div>
        </div>  
    )
}