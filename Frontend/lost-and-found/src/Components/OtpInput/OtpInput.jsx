import { useRef, useState, useEffect } from "react";
import "./OtpInput.css";

const OTP_LENGTH = 6;

/**
 * OTP Input Component with segmented boxes
 * Features: auto-advance, backspace behavior, paste handling
 */
export default function OtpInput({ 
  value = "", 
  onChange, 
  disabled = false,
  hasError = false,
  length = OTP_LENGTH 
}) {
  const [otp, setOtp] = useState(Array(length).fill(""));
  const inputRefs = useRef([]);

  // Sync external value with internal state
  useEffect(() => {
    if (value) {
      const valueArray = value.split("").slice(0, length);
      const paddedArray = [...valueArray, ...Array(length - valueArray.length).fill("")];
      setOtp(paddedArray);
    } else {
      setOtp(Array(length).fill(""));
    }
  }, [value, length]);

  const focusInput = (index) => {
    if (inputRefs.current[index]) {
      inputRefs.current[index].focus();
      inputRefs.current[index].select();
    }
  };

  const handleChange = (e, index) => {
    const inputValue = e.target.value;
    
    // Only allow numeric input
    if (inputValue && !/^\d$/.test(inputValue)) return;

    const newOtp = [...otp];
    newOtp[index] = inputValue;
    setOtp(newOtp);
    
    // Notify parent
    const otpString = newOtp.join("");
    onChange?.(otpString);

    // Auto-advance to next input
    if (inputValue && index < length - 1) {
      focusInput(index + 1);
    }
  };

  const handleKeyDown = (e, index) => {
    // Handle backspace
    if (e.key === "Backspace") {
      e.preventDefault();
      const newOtp = [...otp];
      
      if (otp[index]) {
        // Clear current input
        newOtp[index] = "";
        setOtp(newOtp);
        onChange?.(newOtp.join(""));
      } else if (index > 0) {
        // Move to previous input and clear it
        newOtp[index - 1] = "";
        setOtp(newOtp);
        onChange?.(newOtp.join(""));
        focusInput(index - 1);
      }
    }
    
    // Handle left arrow
    if (e.key === "ArrowLeft" && index > 0) {
      e.preventDefault();
      focusInput(index - 1);
    }
    
    // Handle right arrow
    if (e.key === "ArrowRight" && index < length - 1) {
      e.preventDefault();
      focusInput(index + 1);
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData("text").trim();
    
    // Only allow numeric paste
    if (!/^\d+$/.test(pastedData)) return;
    
    const digits = pastedData.slice(0, length).split("");
    const newOtp = [...Array(length).fill("")];
    
    digits.forEach((digit, i) => {
      newOtp[i] = digit;
    });
    
    setOtp(newOtp);
    onChange?.(newOtp.join(""));
    
    // Focus last filled input or first empty
    const lastFilledIndex = Math.min(digits.length - 1, length - 1);
    focusInput(digits.length < length ? digits.length : lastFilledIndex);
  };

  const handleFocus = (e) => {
    e.target.select();
  };

  return (
    <div className="otp-input-container" dir="ltr">
      {otp.map((digit, index) => (
        <input
          key={index}
          ref={(el) => (inputRefs.current[index] = el)}
          type="text"
          inputMode="numeric"
          maxLength={1}
          value={digit}
          onChange={(e) => handleChange(e, index)}
          onKeyDown={(e) => handleKeyDown(e, index)}
          onPaste={handlePaste}
          onFocus={handleFocus}
          disabled={disabled}
          className={`otp-box ${digit ? "filled" : ""} ${hasError ? "error" : ""}`}
          aria-label={`رقم ${index + 1} از ${length}`}
          autoComplete="one-time-code"
        />
      ))}
    </div>
  );
}

