import { createContext, useContext, useState } from "react";
import { CheckCircle2, AlertCircle, X } from "lucide-react";
const ToastContext = createContext(() => {});
export function useToast() { return useContext(ToastContext); }
export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const notify = (message, type = "success") => {
    const id = Date.now() + Math.random();
    setItems(v => [...v, { id, message, type }]);
    setTimeout(() => setItems(v => v.filter(x => x.id !== id)), 4200);
  };
  return <ToastContext.Provider value={notify}>
    {children}
    <div className="toast-stack">{items.map(item => <div className={`toast toast-${item.type}`} key={item.id}>{item.type === "success" ? <CheckCircle2 size={18}/> : <AlertCircle size={18}/>}<span>{item.message}</span><button onClick={() => setItems(v => v.filter(x => x.id !== item.id))}><X size={15}/></button></div>)}</div>
  </ToastContext.Provider>;
}
