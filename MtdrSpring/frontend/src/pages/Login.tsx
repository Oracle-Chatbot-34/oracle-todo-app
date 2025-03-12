import logo from '../assets/logo.png';
import { useNavigate } from "react-router-dom";

export default function Login() {
    const navigate = useNavigate();

    const handleLogin = () => {
        // Redirect to /home
        console.log("Login successful");
        navigate('/home');

    };

    return(
        <div className="bg-background h-screen w-full flex flex-row items-center justify-center gap-[250px]">
            <div className="flex flex-col items-center">
                <p className="text-[80px]">DashMaster</p>
                <img src={logo} className="w-[500px] h-[500px]" alt="Logo" />
            </div>
            
            <div className="w-[440px] h-[460px] bg-white rounded-lg shadow-xl">
                <br/>
                <div className="flex flex-col items-center gap-[20px]">
                    <div className="justify-center flex flex-col items-center">
                        <p className="text-[40px] w-[230px] text-sm/12 text-center">Go to your Dashboard!</p>
                    </div>

                    <div className="flex flex-col items-start relative gap-[50px]">
                        <div>
                            <p className="text-[20px] text-[#747276] text-left">Email</p>
                            <input 
                            type="text" 
                            style={{ 
                                border: '2px #9B9B9B solid',
                                borderRadius: '8px'  // This will round all corners
                            }}
                            className="w-[350px] h-[40px] border-2 border-solid border-black rounded-lg m-3 p-4"/>
                        </div>
                        <div>
                            <p className="text-[20px] text-[#747276] text-left">Password</p>
                            <input 
                            type="password" 
                            style={{ 
                                border: '2px #9B9B9B solid',
                                borderRadius: '8px'  // This will round all corners
                            }}
                            className="w-[350px] h-[40px] border-2 border-gray-500 rounded-lg"/>
                        </div>
                    </div>
                    <br/>
                    <div className="bg-greenie rounded-lg text-white text-[25px]">
                        <button type="button" className="h-[40px] w-[320px]" onClick={handleLogin}>
                            Login
                        </button>  
                    </div>
                </div>            
            </div>
        </div>  
    )
}