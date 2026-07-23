import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { Eye, EyeOff, Lock, Mail } from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { useAuth } from '../../hooks/useAuth';
import { cn } from '../../lib/utils';

// Carousel slides data
const SLIDES = [
  {
    id: 1,
    title: 'Bienvenue',
    highlight: 'Supervision',
    description: 'Système de supervision temps réel des cabines de peinture, prévention des dérives qualité.',
    image: '/images/p1.png',
  },
  {
    id: 2,
    title: 'Dashboard',
    highlight: 'Temps réel',
    description: 'Suivi en direct des paramètres température et humidité avec visualisations claires.',
    image: '/images/p2.png',
  },
  {
    id: 3,
    title: 'Alertes',
    highlight: 'Intelligentes',
    description: 'Détection via seuils absolus, seuils dynamiques et IA pour une réactivité optimale.',
    image: '/images/p3.png',
  },
  {
    id: 4,
    title: 'Chatbot',
    highlight: 'RAG',
    description: 'Interrogez l\'historique des données en langage naturel grâce à notre chatbot intelligent.',
    image: '/images/p4.png',
  },
];

/**
 * LoginForm – Split-screen with carousel, neumorphism design
 */
const LoginForm: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Auto-slide every 4.5 seconds
  const nextSlide = useCallback(() => {
    setCurrentSlide((prev) => (prev + 1) % SLIDES.length);
  }, []);

  const goToSlide = (index: number) => {
    setCurrentSlide(index);
  };

  useEffect(() => {
    const interval = setInterval(nextSlide, 4500);
    return () => clearInterval(interval);
  }, [nextSlide]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await login(username, password);
      navigate({ to: '/dashboard' });
    } catch {
      setError('Identifiants invalides. Veuillez réessayer.');
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-[#0f172a] overflow-hidden">
      {/* Left Panel - Carousel (Hidden on mobile) */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between p-12 text-white relative overflow-hidden">
        {/* Background Decorations */}
        <div className="absolute inset-0 opacity-20">
          <div className="absolute top-0 left-0 w-64 h-64 rounded-full blur-3xl" style={{ background: 'var(--primary)' }} />
          <div className="absolute bottom-0 right-0 w-96 h-96 rounded-full blur-3xl" style={{ background: 'var(--primary)' }} />
        </div>

        {/* Content */}
        <div className="relative z-10">
          {/* Carousel */}
          <div className="relative h-[32rem]">
            {SLIDES.map((slide, index) => {
              return (
                <div
                  key={slide.id}
                  className={cn(
                    "absolute inset-0 transition-all duration-700 ease-in-out",
                    index === currentSlide
                      ? "opacity-100 translate-x-0"
                      : index < currentSlide
                      ? "opacity-0 -translate-x-full"
                      : "opacity-0 translate-x-full"
                  )}
                >
                  <div className="h-full flex flex-col justify-center">
                    <h2 className="text-4xl font-bold mb-4 leading-tight">
                      {slide.title}{' '}
                      <span style={{ color: 'var(--primary)' }}>{slide.highlight}</span>
                    </h2>
                    <p className="text-lg text-slate-300 leading-relaxed max-w-md mb-6">
                      {slide.description}
                    </p>

                    {/* Image Illustration */}
                    <div className="mt-4 w-full max-w-lg h-80 rounded-3xl overflow-hidden">
                      <img 
                        src={slide.image} 
                        alt={slide.title} 
                        className="w-full h-full object-contain"
                      />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Pagination Dots */}
        <div className="relative z-10 flex items-center gap-3">
          {SLIDES.map((_, index) => (
            <button
              key={index}
              onClick={() => goToSlide(index)}
              className={cn(
                "h-2 rounded-full transition-all duration-300",
                index === currentSlide ? "w-8" : "w-2 bg-white/30 hover:bg-white/50"
              )}
              style={{ background: index === currentSlide ? 'var(--primary)' : undefined }}
              aria-label={`Aller au slide ${index + 1}`}
            />
          ))}
        </div>
      </div>

      {/* Right Panel - Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-12" style={{ background: 'var(--background)' }}>
        <div className="w-full max-w-md neu-card p-8 sm:p-10">
          <div className="text-center mb-12">
            <h1 className="text-3xl font-bold mb-3" style={{ color: 'var(--foreground)' }}>
              Connexion
            </h1>
            <p className="text-muted-foreground">
              Accédez à votre espace de supervision
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-7">
            <div className="space-y-6">
              <Label htmlFor="username" className="text-base">Email</Label>
              <div className="relative">
                <Mail className="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input
                  id="username"
                  type="email"
                  placeholder="votre@email.com"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className={cn(
                    "pl-14 pr-5 h-14 neu-inset border-0 focus-visible:ring-2 text-base placeholder:text-muted-foreground/70",
                    error && "ring-2 ring-destructive"
                  )}
                  required
                />
              </div>
            </div>

            <div className="space-y-6">
              <Label htmlFor="password" className="text-base">Mot de passe</Label>
              <div className="relative">
                <Lock className="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className={cn(
                    "pl-14 pr-14 h-14 neu-inset border-0 focus-visible:ring-2 text-base placeholder:text-muted-foreground/70",
                    error && "ring-2 ring-destructive"
                  )}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            {/* Forgot Password */}
            <div className="flex justify-end">
              <button
                type="button"
                className="text-sm font-medium hover:opacity-80 transition-opacity"
                style={{ color: 'var(--primary)' }}
              >
                Mot de passe oublié ?
              </button>
            </div>

            {/* Error Message */}
            {error && (
              <div className="p-4 rounded-xl text-center text-sm" style={{ 
                background: 'var(--danger-soft)',
                color: 'var(--destructive)'
              }}>
                {error}
              </div>
            )}

            <Button
              type="submit"
              className="w-full h-12 text-base font-semibold transition-all duration-200 hover:scale-[1.01] active:scale-[0.99]"
              style={{ boxShadow: 'var(--shadow-glow)' }}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Connexion en cours...' : 'Se connecter'}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default LoginForm;
