import { provideHttpClient, withFetch } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideArrowDown,
  lucideArrowLeft,
  lucideArrowRight,
  lucideArrowUp,
  lucideCheck,
  lucideCircleAlert,
  lucideCircleCheck,
  lucideCircleX,
  lucideExternalLink,
  lucideGlobe,
  lucideInfo,
  lucideRefreshCw,
  lucideRotateCcw,
  lucideSearch,
  lucideSearchX,
  lucideSkipForward,
  lucideTrophy,
} from '@ng-icons/lucide';

import { routes } from './app.routes';
import { provideZard } from '@/shared/core/provider/providezard';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top' }),
    ),
    provideHttpClient(withFetch()),
    provideZard(),
    provideIcons({
      lucideArrowDown,
      lucideArrowLeft,
      lucideArrowRight,
      lucideArrowUp,
      lucideCheck,
      lucideCircleAlert,
      lucideCircleCheck,
      lucideCircleX,
      lucideExternalLink,
      lucideGlobe,
      lucideInfo,
      lucideRefreshCw,
      lucideRotateCcw,
      lucideSearch,
      lucideSearchX,
      lucideSkipForward,
      lucideTrophy,
    }),
  ],
};
